import os

import numpy as np
import pandas as pd
import scipy
from scipy.signal import welch

path_prefix = "./java-implementation/src/main/resources"


def compute_rmssd(rr_intervals_ms):
    rr_diff = np.diff(rr_intervals_ms)
    rmssd = np.sqrt(np.mean(rr_diff ** 2))
    return rmssd


def __calc_respiration_rate_and_lf_hf_ratio(rr_data):
    t = np.cumsum(rr_data)
    t -= t[0]
    fs = 4
    f = scipy.interpolate.interp1d(t, rr_data, 'cubic')
    t_interpol = np.arange(t[0], t[t.size - 1], 1000. / fs)
    rr_interpol = f(t_interpol)

    frequencies, powers = welch(x=rr_interpol, fs=fs, window='hamming', nfft=2 ** 12, scaling='density')

    lf_band_mask = (frequencies >= 0.04) & (frequencies <= 0.15)
    hf_band_mask = (frequencies >= 0.15) & (frequencies <= 0.4)

    if not np.any(hf_band_mask) or not np.any(lf_band_mask):
        raise ValueError("No frequencies in the LF or HF band")

    freqs_hf = frequencies[hf_band_mask]
    pows_hf = powers[hf_band_mask]

    # Compute respiration rate
    peak_idx = np.argmax(pows_hf)
    peak_freq = freqs_hf[peak_idx]

    resp_rate_bpm = peak_freq * 60.0

    # Compute LF/HF ratio
    lf_freqs = frequencies[lf_band_mask]
    lf_pows = powers[lf_band_mask]

    lf_power = np.trapz(lf_pows, lf_freqs)
    hf_power = np.trapz(pows_hf, freqs_hf)

    lf_hf_ratio = lf_power / hf_power

    return resp_rate_bpm, lf_hf_ratio


def main():
    # Read the patient metadata
    df_info = pd.read_csv(f'{path_prefix}/patient-info.csv')
    df_adults = df_info[df_info['Age (years)'] >= 18]
    results = []  # to store results per patient

    for idx, row in df_adults.iterrows():
        file_id = row['File']  # e.g. 'ID1'
        age = row['Age (years)']
        gender = row['Gender']  # might be NaN or unavailable for some

        rr_file = f'{path_prefix}/{file_id}.txt'
        if not os.path.exists(rr_file):
            print(f"Warning: RR file {rr_file} not found; skipping.")
            continue

        rr_data = np.loadtxt(rr_file)
        rmssd_val = compute_rmssd(rr_data)
        respiration_rate, lf_hf_ratio = __calc_respiration_rate_and_lf_hf_ratio(rr_data)
        results.append({"File": file_id, "Age (years)": age, "Gender": gender, "RMSSD (ms)": rmssd_val,
                        "Resp Rate (bpm)": respiration_rate, "LF/HF Ratio": lf_hf_ratio})

    df_results = pd.DataFrame(results)
    print(df_results)

    df_results.to_csv("analysis_results.csv", index=False)


if __name__ == "__main__":
    main()
