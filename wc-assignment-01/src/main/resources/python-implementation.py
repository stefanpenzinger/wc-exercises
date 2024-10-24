import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import pandas as pd
import scipy

from scipy.fftpack import fft
from scipy.optimize import fminbound


X = pd.read_csv('data/data2.csv')

fs = 100            # sampling frequency
N = 512             # window size fft
ts = 1.25           # duration of the sliding window
res = fs / N        # resolution of the fft
ls = int(ts * fs)   # length of the sliding windows

i = 0
stepCount = 0

while i+N < len(X):

    window = X.iloc[i:i+N, 1:4]
    mean_values = window.abs().mean()
    max_axis = mean_values.idxmax()

    match max_axis:
      case "wx":
          idx = 0
      case "wy":
          idx = 1
      case "wz":
          idx = 2
      case _:
          idx = 0

    S = 2*abs(fft(X.iloc[i:i+N, idx+1].to_numpy()))

    w0 = np.mean(S[0:2])
    wc = np.mean(S[2:7])

    coefficients = np.polyfit([1, 2, 3, 4, 5], S[2:7], 4)
    def f(x): return -np.polyval(coefficients, x)
    maximum = fminbound(f, 1, 5)
    #maximum = scipy.optimize.brent(f, brack=(1, 5))
    if wc > w0 and wc > 10:
        fw = res * (maximum + 1)
        c = ts * fw
        stepCount = stepCount + c

    i = i + ls

print("Step count: ", stepCount)