import m2cgen
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
from joblib import load, dump
from pandas import DataFrame
from scipy import stats
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error
from sklearn.model_selection import train_test_split, GridSearchCV

model_name = "random_forest_model.joblib"
java_out_file = "./java-implementation/src/Model.java"

def __load_dataframe() -> DataFrame:
    subject_df = pd.read_csv('data/subject-info.csv')
    measure_df = pd.read_csv('data/test_measure.csv')

    # Task 1
    ## Merge dataframes
    df = pd.merge(measure_df, subject_df[['ID_test', 'Sex', 'Age', 'Weight']], on='ID_test', how='left',
                  validate="many_to_many")
    ## Remove missing values
    df = df.dropna()
    ## Fix sex column
    df['Sex'] = df['Sex'].replace({0: 1, 1: 0})

    ## Remove outliers
    df['z_score'] = stats.zscore(df['HR'])
    return df[(df['z_score'] < 3) & (df['z_score'] > -3)]


def __calc_weir(weir_df: DataFrame) -> DataFrame:
    # Task 2
    ## Dataset contains only O2 and VO2 measurements, compute EE with Weir’s equation
    weir_df['VO2_used'] = weir_df['VO2'] / 1000
    weir_df['VCO2_used'] = weir_df['VCO2'] / 1000
    weir_df['EE_Weir'] = (3.9 * weir_df['VO2_used'] + 1.1 * weir_df['VCO2_used']) * 4.186

    return weir_df


def __calc_keytel(keytel_df: DataFrame) -> DataFrame:
    # Task 3
    ## Get VO2_max
    keytel_df['VO2_max'] = keytel_df.groupby('ID_test')['VO2'].transform('max') / keytel_df['Weight']

    ## Compute EE with Keytel’s equation
    keytel_df['EE_Keytel'] = (
            -59.3954 +
            keytel_df['Sex'] *
            (
                    -36.3781 + 0.271 * keytel_df['Age'] + 0.394 * keytel_df['Weight'] + 0.404 *
                    keytel_df['VO2_max'] + 0.634 * keytel_df['HR']
            ) +
            (
                    1 - keytel_df['Sex']
            ) *
            (
                    0.274 * keytel_df['Age'] + 0.103 * keytel_df['Weight'] + 0.380 *
                    keytel_df['VO2_max'] + 0.450 * keytel_df['HR']
            )
    )

    return keytel_df


def __plot_regression_model(x_train, x_test, y_train, y_test, title):
    regressor = LinearRegression()
    regressor.fit(x_train, y_train)
    y_pred = regressor.predict(x_test)

    # Scatter plot: Measured vs. Predicted EE
    plt.figure(figsize=(10, 6))
    sns.scatterplot(x=y_test, y=y_pred, color='blue', label='Predicted vs Measured')

    # Plot ideal line (y = x)
    plt.plot([y_test.min(), y_test.max()], [y_test.min(), y_test.max()], 'k--', lw=2, label='Ideal Fit')

    # Labels and title
    plt.xlabel('Measured Energy Expenditure (EE)')
    plt.ylabel('Predicted Energy Expenditure (EE)')
    plt.title(f'Measured vs. Predicted Energy Expenditure - {title}')
    plt.legend()
    plt.show()

def __train_random_forest_with_fixed_parameters(X_train, X_test, y_train, y_test):
    # ---- Train RandomForest with best params ----
    best_rf = RandomForestRegressor(
        max_depth=7,
        min_samples_leaf=1,
        min_samples_split=2,
        n_estimators=60,
        random_state=42
    )
    best_rf.fit(X_train, y_train)

    # ---- Evaluate model ----
    y_pred = best_rf.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    print(f"Mean Squared Error on test set = {mse}")

    # ---- Save the model to disk ----
    dump(best_rf, model_name)
    print("Model saved to 'random_forest_model.joblib'.")


def __random_forest(x_train, x_test, y_train, y_test, title):
    param_grid = {
        'n_estimators': [20, 40, 60],  # Number of trees in the forest
        'max_depth': [3, 5, 7],  # Maximum depth of the trees
        'min_samples_split': [2, 5, 10],  # Minimum number of samples required to split a node
        'min_samples_leaf': [1, 2, 4],  # Minimum number of samples required at each leaf node
    }

    rf = RandomForestRegressor(random_state=42)
    grid_search = GridSearchCV(estimator=rf, param_grid=param_grid,
                               cv=5, scoring='neg_mean_squared_error',
                               n_jobs=-1, verbose=2)

    grid_search.fit(x_train, y_train)
    # Extract the best estimator
    best_rf = grid_search.best_estimator_

    # Make predictions
    y_pred = best_rf.predict(x_test)

    # Calculate the performance metric
    mse = mean_squared_error(y_test, y_pred)
    print(f"Mean Squared Error: {mse}")

    print("Best parameters found: ", grid_search.best_params_)

    # --- SAVE MODEL TO DISK ---
    # This saves the best random forest model to "random_forest_model.joblib"
    dump(best_rf, "random_forest_model.joblib")
    print("Model saved to 'random_forest_model.joblib'")

def __save_to_java():
    best_rf = load(model_name)
    java_code = m2cgen.export_to_java(best_rf)
    with open(java_out_file, "w") as f:
        f.write(java_code)

    print("Java code has been generated in '", java_out_file, "'.")

if __name__ == "__main__":
    df = __load_dataframe()
    df = __calc_weir(df)
    df = __calc_keytel(df)

    # X = df[['HR']]
    X = df[['Age', 'VO2_max', 'Weight', 'Sex', 'HR']]

    X_train_weir, X_test_weir, y_train_weir, y_test_weir = train_test_split(X, df['EE_Weir'], train_size=0.75,
                                                                            random_state=12345)
    X_train_keytel, X_test_keytel, y_train_keytel, y_test_keytel = train_test_split(X, df['EE_Keytel'], train_size=0.75,
                                                                                    random_state=12345)

    # __plot_regression_model(X_train_weir, X_test_weir, y_train_weir, y_test_weir, "Weir")
    # __plot_regression_model(X_train_keytel, X_test_keytel, y_train_keytel, y_test_keytel, "Weir")
    #__random_forest(X_train_weir, X_test_weir, y_train_weir, y_test_weir, "Weir")
    # __random_forest(X_train_keytel, X_test_keytel, y_train_keytel, y_test_keytel)

    #__train_random_forest_with_fixed_parameters(X_train_weir, X_test_weir, y_train_weir, y_test_weir)
    __save_to_java()

    #
    # # The coefficients
    # print("Coefficients: \n", regressor.coef_)
    # # The mean squared error
    # print("Mean squared error: %.2f" % mean_squared_error(y_test, y_pred))
    # # The coefficient of determination: 1 is perfect prediction
    # print("Coefficient of determination: %.2f" % r2_score(y_test, y_pred))
    #
    # # Plot outputs
    # plt.scatter(X_test, y_test, color="black")
    # plt.plot(X_test, y_pred, color="blue", linewidth=3)
    #
    # plt.xticks(())
    # plt.yticks(())
    #
    # plt.show()

    # weir_poly1d = np.polyfit(X, df['EE_Weir'], 1)

    # keytel_model = LinearRegression().fit(X, df['EE_Keytel'])
    # keytel_poly1d = np.polyfit(X, df['EE_Keytel'], 1)

    # linregress(df['EE_Weir'], df['EE_Keytel'])
