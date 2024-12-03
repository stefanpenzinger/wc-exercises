# Wearable Computing Assignment 03

Based on the [dataset](https://physionet.org/content/treadmill-exercise-cardioresp/1.0.1/) from the university of
Malaga, a regressor to predict Energy Expenditure (EE) from Age, VO2max, Mass, Gender and Heart Rate (HR) should be
developed.

1. Derive a simple linear regression equation and compare it to the results achieved with the equation from Keytel et
   al.
2. Since the dataset contains only O2 and CO2 measurements, compute EE with Weir’s equation (`total kcal = 3.9 * litres
   O2 used + 1.1 * litres CO2 produced`) first.
3. Try more advanced regressors such as random forests and compare them to Keytel and your own linear regression.
4. Finally, implement the best performing regressor on a platform of your choice.