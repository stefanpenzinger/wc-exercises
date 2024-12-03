import pandas as pd
from pandas import DataFrame
from scipy import stats
from sklearn.linear_model import LinearRegression


def __load_dataframe() -> DataFrame:
    subject_df = pd.read_csv('data/subject-info.csv')
    measure_df = pd.read_csv('data/test_measure.csv')

    # Task 1
    ## Merge dataframes
    df = pd.merge(measure_df, subject_df[['ID_test', 'Sex', 'Age', 'Weight']], on='ID_test', how='left', validate="many_to_many")
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
    keytel_df['EE_Keytel'] =    (
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


if __name__ == "__main__":
    df = __load_dataframe()
    df = __calc_weir(df)
    df = __calc_keytel(df)

    X = df[['Age', 'VO2_max', 'Weight', 'Sex', 'HR']]

    weir_mode = LinearRegression().fit(X, df['EE_Weir'])
    keytel_model = LinearRegression().fit(X, df['EE_Keytel'])

    


