from flask import Flask, Response, request, jsonify
from flask_cors import CORS
import json

app = Flask(__name__)
CORS(app)

user={
    'shbin05':{
        "password": "qwer1234",
        "carName": "아반떼",
        "company": "현대",
        "carYear": "2016"
    }
}

@app.route('/')
def index():
    return 'SCA_API'

@app.route('/register', methods=['POST'])
def register():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        carName = data['carName']
        company = data['company']
        carYear = data['carYear']

        user[username] = {
            'password': password,
            'carName': carName,
            'company': company,
            'carYear': carYear
        }
        print(user)
        return jsonify("success")

@app.route('/login', methods=['POST'])
def login():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        if username in user:
            if password == user[username]['password']:
                return jsonify({
                    "success": True,
                    "username": username
                })
            else:
                return jsonify({
                    "success": False
                })
        else:
            return jsonify({
                "success": False
            })

@app.route('/config', methods=['POST'])
def config():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        if username in user:
            return jsonify("fail")
        else:
            return jsonify("success")
        
@app.route('/userinfo', methods=['POST'])
def userinfo():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        if username in user:
            return jsonify({
                "success": True,
                "username": username,
                "password": user[username]["password"],
                "carName": user[username]["carName"],
                "company": user[username]["company"],
                "carYear": user[username]["carYear"] 
            })
        else:
            return jsonify({
                "success": False
            })

@app.route('/changeinfo', methods=['POST'])
def changeinfo():
    if request.method == 'POST':
        data = request.get_json()
        username = data['username']
        password = data['password']
        carName = data['carName']
        company = data['company']
        carYear = data['carYear']
        
        user[username]['password'] = password
        user[username]['carName'] = carName
        user[username]['company'] = company
        user[username]['carYear'] = carYear
        
        return jsonify("success")

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=8000)
