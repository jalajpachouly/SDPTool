import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

def plot_model_specific_comparison(metrics_dict):
    """
    Plots a model-specific comparison of evaluation metrics for balanced vs. unbalanced datasets.

    Parameters:
    - metrics_dict (dict): Nested dictionary containing evaluation metrics for each model under both conditions.
    """
    data = []
    for dataset, models in metrics_dict.items():
        for model, metrics in models.items():
            for metric, score in metrics.items():
                data.append({
                    'Dataset': dataset,
                    'Model': model,
                    'Metric': metric,
                    'Score': score
                })

    # Create DataFrame
    df = pd.DataFrame(data)

    # Specify the order for consistent plotting
    model_order = ['Logistic Regression', 'Linear SVM', 'Multinomial NB', 'Random Forest', 'BERT']
    metric_order = ['Accuracy', 'Precision', 'Recall', 'F1-Score']
    dataset_order = ['Unbalanced', 'Balanced']

    # Set the style
    sns.set(style="whitegrid")

    # Create a figure for box plots
    plt.figure(figsize=(16, 8))
    sns.boxplot(
        data=df,
        x='Metric',
        y='Score',
        hue='Dataset',
        palette='Set2',
        order=metric_order
    )
    plt.title('Evaluation Metrics Comparison Across Datasets')
    plt.xlabel('Evaluation Metric')
    plt.ylabel('Score')
    plt.legend(title='Dataset')
    plt.tight_layout()
    plt.savefig('evaluation_metrics_boxplot.png')
    plt.show()

    # Create a figure for bar plots
    g = sns.catplot(
        data=df,
        kind='bar',
        x='Model',
        y='Score',
        hue='Dataset',
        col='Metric',
        col_order=metric_order,
        hue_order=dataset_order,
        order=model_order,
        palette='Set2',
        height=4,
        aspect=0.7
    )
    g.fig.subplots_adjust(top=0.9)
    g.fig.suptitle('Model-Specific Bar Plot Comparison for Balanced vs. Unbalanced Data')
    g.set_axis_labels("Model", "Score")
    for ax in g.axes.flatten():
        ax.tick_params(axis='x', rotation=45)
    plt.tight_layout()
    plt.savefig('model_comparison_barplot.png')
    plt.show()

# Example usage
# Define your evaluation metrics in a nested dictionary
# Updated evaluation metrics based on your data
metrics_dict = {
    'Unbalanced': {
        'Logistic Regression': {'Accuracy': 0.67, 'Precision': 0.72, 'Recall': 0.67, 'F1-Score': 0.63},
        'Linear SVM': {'Accuracy': 0.80, 'Precision': 0.81, 'Recall': 0.80, 'F1-Score': 0.79},
        'Multinomial NB': {'Accuracy': 0.50, 'Precision': 0.32, 'Recall': 0.50, 'F1-Score': 0.33},
        'Random Forest': {'Accuracy': 0.76, 'Precision': 0.77, 'Recall': 0.76, 'F1-Score': 0.74},
        'BERT': {'Accuracy': 0.85, 'Precision': 0.86, 'Recall': 0.85, 'F1-Score': 0.85}
    },
    'Balanced': {
        'Logistic Regression': {'Accuracy': 0.79, 'Precision': 0.79, 'Recall': 0.79, 'F1-Score': 0.79},
        'Linear SVM': {'Accuracy': 0.80, 'Precision': 0.80, 'Recall': 0.80, 'F1-Score': 0.80},
        'Multinomial NB': {'Accuracy': 0.71, 'Precision': 0.75, 'Recall': 0.71, 'F1-Score': 0.72},
        'Random Forest': {'Accuracy': 0.77, 'Precision': 0.78, 'Recall': 0.77, 'F1-Score': 0.77},
        'BERT': {'Accuracy': 0.89, 'Precision': 0.89, 'Recall': 0.89, 'F1-Score': 0.88}
    }
}

# Plot the comparison
plot_model_specific_comparison(metrics_dict)
