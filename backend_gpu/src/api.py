from flask import Flask, Response, request, jsonify
from flask_cors import CORS

from ultralytics import YOLO
from Model_UNET import Unet
from Model_VGG19 import IntelCnnModel
import Utils_GradientBoosting
import torch
import joblib
import numpy as np
import cv2
from PIL import Image
import skimage
from skimage import color
from scipy import ndimage
from torchvision.transforms import ToTensor
from scipy import ndimage
from pycocotools.coco import COCO
import albumentations as A
import torch.nn.functional as F
from io import BytesIO
import base64

import os
import json
from datetime import datetime

INPUT_PATH = '../input/'
OUTPUT_PATH = '../output/'

PART_WEIGHT = '../weights/part/Part.pt'

BREAKAGE_WEIGHT = '../weights/damage/Breakage.pt'
CRUSHED_WEIGHT = '../weights/damage/Crushed.pt'
SCRATCHED_WEIGHT = '../weights/damage/Scratched.pt'
SEPARATED_WEIGHT = '../weights/damage/Separated.pt'

REPAIR_METHOD_WEIGHT = '../weights/repair_method/repair_method_vgg19.pth'
REPAIR_COST_WEIGHT = '../weights/repair_cost/repair_cost_GradientBoostingRegressor.pkl'

DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'

app = Flask(__name__)
CORS(app)

@app.route('/')
def index():
    return 'SCA_API'

global origImage

def overlap(rect1, rect2):
     
    # if rectangle has area 0, no overlap
    if rect1[0] == rect1[2] or rect1[1] == rect1[3] or rect2[2] == rect2[0] or rect2[1] == rect2[3]:
        return False
     
    # If one rectangle is on left side of other
    if rect1[0] > rect2[2] or rect2[0] > rect1[2]:
        return False
 
    # If one rectangle is above other
    if rect1[3] < rect2[1] or rect2[3] < rect1[1]:
        return False
 
    return True

def make_part_predictions(model, origImage):
    parts = ["FrontBumper","RearBumper","FrontFender(R)","FrontFender(L)","RearFender(R)","TrunkLid","Bonnet","RearFender(L)","RearDoor(R)","HeadLights(R)","HeadLights(L)","FrontWheel(R)","FrontDoor(R)","SideMirror(R)", "Damage"]
    damage_indices = []
    damaged_part_cls = []
    damaged_part_coor = []
    damaged_part_conf = []

    results = model.predict(origImage)
    results = results[0].cpu().numpy()

    classes = results.boxes.cls.astype(int)
    boxes = results.boxes.xyxy.astype(int)
    conf = results.boxes.conf

    for num, cls in enumerate(classes):
        if cls == 14:
            damage_indices.append(num)

    for dmg_idx in damage_indices:
        for idx, cls in enumerate(classes):
            if cls != 14 and overlap(boxes[dmg_idx],boxes[idx]) and parts[cls] not in damaged_part_cls and not "HeadLights" in parts[cls]:
                damaged_part_cls.append(parts[cls])
                damaged_part_coor.append(boxes[idx])
                damaged_part_conf.append(conf[idx])

    return damaged_part_cls, damaged_part_coor, damaged_part_conf

def find_damage(predMask):
    # Find and print car part objects
    data_slices = ndimage.find_objects(predMask)
    try:
        if ((damage := data_slices[0]) != None):

            y_start = damage[0].start
            y_stop = damage[0].stop

            x_start = damage[1].start
            x_stop = damage[1].stop

            cut = predMask[y_start:y_stop+1,x_start:x_stop+1,:]

            val = round(cut.size/(256*256) * 100, 1)
            if val < 0.1: 
                return None
            else:
                return val 

    except:
        return None


def make_damage_predictions(model1, model2, model3, model4, part_img):
    model1.eval()
    model2.eval()
    model3.eval()
    model4.eval()
    with torch.no_grad():
    
        tf_toTensor = ToTensor()
        image = tf_toTensor(part_img).float().to(DEVICE)

        predMask=[0,0,0,0]

        predMask[0] = model1(image.unsqueeze(0))
        predProb1 = F.softmax(predMask[0], dim = 1).detach().cpu().numpy()
        predMask[0] = torch.argmax(predMask[0], dim=1).detach().cpu().numpy()

        predMask[1] = model2(image.unsqueeze(0))
        predProb2 = F.softmax(predMask[1], dim = 1).detach().cpu().numpy()
        predMask[1] = torch.argmax(predMask[1], dim=1).detach().cpu().numpy()

        predMask[2] = model3(image.unsqueeze(0))
        predProb3 = F.softmax(predMask[2], dim = 1).detach().cpu().numpy()
        predMask[2] = torch.argmax(predMask[2], dim=1).detach().cpu().numpy()

        predMask[3] = model4(image.unsqueeze(0))
        predProb4 = F.softmax(predMask[3], dim = 1).detach().cpu().numpy()
        predMask[3] = torch.argmax(predMask[3], dim=1).detach().cpu().numpy()

        sum = [0,0,0,0]
        count = [0,0,0,0]
        for x in range(predMask[0].shape[1]):
            for y in range(predMask[0].shape[2]):
                if (predMask[0][0,x,y]):
                    count[0] += 1
                    sum[0] += predProb1[0,1,x,y]
                if (predMask[1][0,x,y]):
                    count[1] += 1
                    sum[1] += predProb2[0,1,x,y]
                if (predMask[2][0,x,y]):
                    count[2] += 1
                    sum[2] += predProb3[0,1,x,y]
                if (predMask[3][0,x,y]):
                    count[3] += 1
                    sum[3] += predProb4[0,1,x,y]

        val=[0,0,0,0]

        predMask[0] = np.transpose(predMask[0], (1,2,0))
        val[0] = find_damage(predMask[0])

        predMask[1] = np.transpose(predMask[1], (1,2,0))
        val[1] = find_damage(predMask[1])

        predMask[2] = np.transpose(predMask[2], (1,2,0))
        val[2] = find_damage(predMask[2])

        predMask[3] = np.transpose(predMask[3], (1,2,0))
        val[3] = find_damage(predMask[3])

    return (predMask, val, sum, count)

def damage_drawMask(part_img, damageMask):
    for x in range(damageMask.shape[0]):
        for y in range(damageMask.shape[1]):
            if (damageMask[x,y,0] == 0 and damageMask[x,y,1] == 0 and damageMask[x,y,2] == 0):
                r, g, b = part_img.getpixel((y, x))
                damageMask[x,y,0] = r/255
                damageMask[x,y,1] = g/255
                damageMask[x,y,2] = b/255
    return damageMask

def str_to_img(str):
    str = base64.b64decode(str)    
    img =  BytesIO(str)
    img = Image.open(img)
    return img

def img_to_str(img):
    img_buffer = BytesIO()
    img.save(img_buffer, format="PNG")
    img_str = base64.b64encode(img_buffer.getvalue()).decode('utf8')
    
    return img_str

def skimage_to_str(img):
    pill_img = Image.fromarray(skimage.img_as_ubyte(img))
    img_buffer = BytesIO()
    pill_img.save(img_buffer, format="PNG")
    img_str = base64.b64encode(img_buffer.getvalue()).decode('utf8')
    return img_str

@app.route('/api/main', methods=['POST'])
def main():
    response = Response()
    if request.method == 'POST':
        data = request.get_json()
        img_string = data["img_string"].replace("data:image/jpeg;base64,","").replace("data:image/jpg;base64,","").replace("data:image/png;base64,","")
        origImage = str_to_img(img_string)
        origImage = origImage.resize((256, 256))
        
        firstday = int(data["firstday"]+"0101")
        repairday = int(datetime.today().strftime("%Y%m%d"))
        company = data["company"]
        
        data={}
        
        data["origImage"] = img_to_str(origImage)
        
        #part_prediction
        parts, part_coor, conf = make_part_predictions(model, origImage)
        print("Damaged Parts: "+', '.join(parts))
        data["part"] = parts

        data["info"] = []
        
        for i in range(len(parts)):
            dict={}
            print("\nDetecting damage in "+parts[i]+"...\n")
            crop = origImage.crop(part_coor[i])
            width, height = crop.size
            part_img = Image.new(crop.mode, (256,256), (255, 255, 255))
            part_img.paste(crop, (int((256-width)/2), int((256-height)/2)))
            dict["part"] = parts[i]
            dict["part_img"]= img_to_str(part_img)
            
            damage_mask, val, sum, count = make_damage_predictions(model1, model2, model3, model4, part_img)
            dict["damage_mask"]=[]
            
            for j in range(4):
                dict['damage_mask'].append(skimage_to_str(damage_drawMask(part_img, color.label2rgb(damage_mask[j][:,:,0]))))

            dict['damage_info']=[]
            dict['checkbox_info']={}
            labels = ['Breakage', 'Crushed', 'Scratched', 'Separated']
            for j in range(4):
                if val[j] is None: 
                    print(labels[j]+": Damage is not detected")
                    dict['damage_info'].append(labels[j]+": Not detected")
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_disable']=True
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_color']="grey"
                else: 
                    print(labels[j]+": "+str(val[i])+"% area")
                    print(labels[j] + " confidence score: "+ str(round((sum[j]/count[j]) * 100, 1)) + "%")
                    dict['damage_info'].append(labels[j] + ": Detected, Confidence score: "+ str(round((sum[j]/count[j]) * 100, 1)) + "%")
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_disable']=False
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_color']="green"
                print("")
            
            repair_method = get_repair_method(repair_method_model, part_img)
            print(parts[i]+" repair method is "+ repair_method)
            dict['repair_method'] = repair_method
            
            modelinput, repair_cost = Utils_GradientBoosting.get_model_input(firstday, repairday, company, parts[i], repair_method)
            repair_cost += repair_cost_model.predict([modelinput])[0]
            repair_cost = int(round(repair_cost, -3))
            print(parts[i]+" repair cost is "+ str(repair_cost)+ " won")
            dict['repair_cost'] = str(repair_cost)

            print("\n"+"-"*40)
            
            data['info'].append(dict)
        
        response = jsonify(data)
        response.headers.add("Access-Control-Allow-Origin", "*")
        response.headers['Content-Type'] = 'application/json'
        return response

@app.route('/api/test', methods=['POST'])
def test():
    response = Response()
    if request.method == 'POST':
        response.headers.add("Access-Control-Allow-Origin", "*")
        data = request.get_json()
        
        firstday = 20170101
        repairday = int(datetime.today().strftime("%Y%m%d"))
        company = "현대"
        
        idx = int(data["index"])
        data={}
        
        #load original mask
        coco = COCO('../testset/datainfo/testset_info.json')
        img_ids = coco.getImgIds()
        image_id = int(img_ids[idx])
        image_infos = coco.loadImgs(image_id)[0]
        images = cv2.imread(os.path.join('../testset/img/', image_infos['file_name']))
        images = cv2.cvtColor(images, cv2.COLOR_BGR2RGB)
        ann_ids = coco.getAnnIds(imgIds=image_infos['id'])
        anns = coco.loadAnns(ann_ids)
        masks = np.zeros((image_infos["height"], image_infos["width"]))
        for ann in anns:
            pixel_value = ann['category_id'] + 1
            masks = np.maximum(coco.annToMask(ann) * pixel_value, masks)
        resize = A.Compose([A.Resize(width=256, height=256)])
        transformed = resize(image = images, mask=masks)
        origMask = transformed["mask"]

        #load original image
        origImage = Image.open(os.path.join('../testset/img/', image_infos['file_name']))
        origImage = origImage.resize((256, 256))
        
        data["origImage"] = img_to_str(origImage)
        data["origMask"] = skimage_to_str(damage_drawMask(origImage, color.label2rgb(origMask)))
        
        #part_prediction
        parts, part_coor, conf = make_part_predictions(model, origImage)
        print("Damaged Parts: "+', '.join(parts))
        data["part"] = parts

        data["info"] = []
        
        for i in range(len(parts)):
            dict={}
            print("\nDetecting damage in "+parts[i]+"...\n")
            crop = origImage.crop(part_coor[i])
            width, height = crop.size
            part_img = Image.new(crop.mode, (256,256), (255, 255, 255))
            part_img.paste(crop, (int((256-width)/2), int((256-height)/2)))
            dict["part"] = parts[i]
            dict["part_img"]= img_to_str(part_img)
            
            damage_mask, val, sum, count = make_damage_predictions(model1, model2, model3, model4, part_img)
            dict["damage_mask"]=[]
            
            for j in range(4):
                dict['damage_mask'].append(skimage_to_str(damage_drawMask(part_img, color.label2rgb(damage_mask[j][:,:,0]))))

            dict['damage_info']=[]
            dict['checkbox_info']={}
            labels = ['Breakage', 'Crushed', 'Scratched', 'Separated']
            for j in range(4):
                if val[j] is None: 
                    print(labels[j]+": Damage is not detected")
                    dict['damage_info'].append(labels[j]+": Not detected")
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_disable']=True
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_color']="grey"
                else: 
                    print(labels[j]+": "+str(val[i])+"% area")
                    print(labels[j] + " confidence score: "+ str(round((sum[j]/count[j]) * 100, 1)) + "%")
                    dict['damage_info'].append(labels[j] + ": Detected, Confidence score: "+ str(round((sum[j]/count[j]) * 100, 1)) + "%")
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_disable']=False
                    dict['checkbox_info'][parts[i]+'_'+labels[j]+'_color']="green"
                print("")
            
            repair_method = get_repair_method(repair_method_model, part_img)
            print(parts[i]+" repair method is "+ repair_method)
            dict['repair_method'] = repair_method
            
            modelinput, repair_cost = Utils_GradientBoosting.get_model_input(firstday, repairday, company, parts[i], repair_method)
            repair_cost += repair_cost_model.predict([modelinput])[0]
            repair_cost = int(round(repair_cost, -3))
            print(parts[i]+" repair cost is "+ str(repair_cost)+ " won")
            dict['repair_cost'] = str(repair_cost)

            print("\n"+"-"*40)
            
            data['info'].append(dict)

        response.set_data(json.dumps(data))
        return response

def load_part_yolo_model(weight_path):
    model = YOLO(weight_path)

    return model

def load_damage_unet_model(weight_path):
    model = Unet(encoder="resnet34",pre_weight='imagenet',num_classes=2)
    model = model.to(DEVICE)
    try:
        model.model.load_state_dict(torch.load(weight_path, map_location=torch.device('cuda')))
        return model.model
    except:
        try:
            model.load_state_dict(torch.load(weight_path, map_location=torch.device('cpu')))
            return model
        except:
            model.load_state_dict(torch.load(weight_path, map_location=torch.device('cpu')), strict=False)
            return model

def get_repair_method(model, img):
    tf_toTensor = ToTensor()
    image = tf_toTensor(img).float().to(DEVICE)
    
    predictions = model(image.unsqueeze(0))
    prediction = predictions[0].detach().cpu()
    cl = int(np.argmax(prediction))
    
    if cl==0: method = 'detach'
    elif cl==1: method = 'overhaul'
    else: method = 'replace'
    
    return method

if __name__ == '__main__':
    print("Loading models...")
    #load part model
    model = load_part_yolo_model(weight_path=PART_WEIGHT)

    #load damage model
    model1 = load_damage_unet_model(weight_path=BREAKAGE_WEIGHT)
    model2 = load_damage_unet_model(weight_path=CRUSHED_WEIGHT)
    model3 = load_damage_unet_model(weight_path=SCRATCHED_WEIGHT)
    model4 = load_damage_unet_model(weight_path=SEPARATED_WEIGHT)

    #load repair method model
    repair_method_model = torch.load(REPAIR_METHOD_WEIGHT, map_location=torch.device('cuda'))
    
    #load repair cost model
    repair_cost_model = joblib.load(REPAIR_COST_WEIGHT)
    
    app.run(host="0.0.0.0", port=8000)