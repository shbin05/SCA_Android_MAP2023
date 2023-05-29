import torch

DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'
print(DEVICE)