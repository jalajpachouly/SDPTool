import os
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt


def plot_model_specific_comparison(metrics_dict, metrics_to_plot=None, output_dir="../output"):
    """
    Plots a model-specific comparison of evaluation metrics for balanced vs. unbalanced datasets.
    Displays two metrics at a time with corresponding box plots and bar plots.

    Parameters:
    - metrics_dict (dict): Nested dictionary containing evaluation metrics for each model under both conditions.
    - metrics_to_plot (list): List of metrics to plot. If None, plots all available metrics.
    - output_dir (str): Path to the directory where output images will be saved.
    """
    # Ensure the output directory exists; create it if it doesn't
    os.makedirs(output_dir, exist_ok=True)

    data = []
    for dataset, models in metrics_dict.items():
        for model, metrics in models.items():
            for metric, score in metrics.items():
                # Only include the specified metrics
                if metrics_to_plot is None or metric in metrics_to_plot:
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
    dataset_order = ['Unbalanced', 'Balanced']

    # Determine the metrics to plot
    if metrics_to_plot is not None:
        metric_order = metrics_to_plot
    else:
        metric_order = ['Accuracy', 'Precision', 'Recall', 'F1-Score']

    # Group metrics in pairs
    grouped_metrics = [metric_order[i:i + 2] for i in range(0, len(metric_order), 2)]

    # Set the style
    sns.set(style="whitegrid")

    for group in grouped_metrics:
        num_metrics = len(group)
        # Determine subplot layout
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        plt.subplots_adjust(wspace=0.3, hspace=0.4)

        # Flatten axes for easy iteration
        axes = axes.flatten()

        # --- Box Plots ---
        for i, metric in enumerate(group):
            ax = axes[i]
            sns.boxplot(
                data=df[df['Metric'] == metric],
                x='Metric',
                y='Score',
                hue='Dataset',
                palette='Set2',
                order=[metric],
                ax=ax
            )
            ax.set_title(f'Box Plot for {metric}')
            ax.set_xlabel('Evaluation Metric')
            ax.set_ylabel('Score')
            # Remove the legend to add a single centralized legend later
            ax.legend_.remove()
            # Rotate x-axis labels if needed
            ax.tick_params(axis='x', rotation=45)

        # --- Bar Plots ---
        for i, metric in enumerate(group):
            ax = axes[i + num_metrics]
            sns.barplot(
                data=df[df['Metric'] == metric],
                x='Model',
                y='Score',
                hue='Dataset',
                order=model_order,
                hue_order=dataset_order,
                palette='Set2',
                ax=ax
            )
            ax.set_title(f'Bar Plot for {metric}')
            ax.set_xlabel('Model')
            ax.set_ylabel('Score')
            # Remove the legend to add a single centralized legend later
            ax.legend_.remove()
            # Rotate x-axis labels for better readability
            ax.tick_params(axis='x', rotation=45)

        # Create Legends:
        # Extract handles and labels from the first box plot and first bar plot
        box_handles, box_labels = axes[0].get_legend_handles_labels()
        bar_handles, bar_labels = axes[num_metrics].get_legend_handles_labels()

        # Add a single legend for box plots
        box_legend = fig.legend(
            box_handles,
            box_labels,
            title='Dataset (Box Plots)',
            loc='upper right',
            bbox_to_anchor=(0.95, 0.85)
        )

        # Add a single legend for bar plots
        bar_legend = fig.legend(
            bar_handles,
            bar_labels,
            title='Dataset (Bar Plots)',
            loc='upper right',
            bbox_to_anchor=(0.95, 0.70)
        )

        # Set the main title for the figure based on the metrics
        if len(group) == 2:
            title = f'Evaluation Metrics: {group[0]} and {group[1]}'
        else:
            title = f'Evaluation Metric: {group[0]}'
        fig.suptitle(title, fontsize=16)

        # Save the figure with tight bounding box in the output directory
        filename = f'evaluation_metrics_comparison_{"_and_".join(group)}.png'
        filepath = os.path.join(output_dir, filename)
        plt.tight_layout(rect=[0, 0, 0.90, 0.95])  # Adjust rect to make room for legends and title
        plt.savefig(filepath, bbox_inches='tight')
        plt.show()

    # Optionally, create a comprehensive bar plot with all metrics, two at a time
    # Uncomment the following block if needed

    """
    # Create a figure for bar plots with all metrics, two per figure
    for group in grouped_metrics:
        aspect_ratio = 0.7
        figure_width = 16  # Adjust as needed

        # Create a catplot for each group of metrics
        g = sns.catplot(
            data=df[df['Metric'].isin(group)],
            kind='bar',
            x='Model',
            y='Score',
            hue='Dataset',
            col='Metric',
            col_order=group,
            hue_order=dataset_order,
            order=model_order,
            palette='Set2',
            height=5,
            aspect=aspect_ratio,
            legend=False  # Disable the default legend
        )

        # Adjust the top to make space for the suptitle and right to make space for the legend
        g.fig.subplots_adjust(top=0.85, right=0.8)

        # Set the suptitle with metric names
        g.fig.suptitle(f'Model-Specific Bar Plot Comparison: {" and ".join(group)}')

        # Rotate x-axis labels for better readability
        for ax in g.axes.flatten():
            ax.tick_params(axis='x', rotation=45)

        # Add a single legend outside the plot
        handles, labels = g.axes.flatten()[0].get_legend_handles_labels()
        g.fig.legend(handles, labels, title='Dataset', loc='center right', bbox_to_anchor=(0.95, 0.5))

        # Save and show the plot in the output directory
        filename = f'model_comparison_barplot_{"_and_".join(group)}.png'
        filepath = os.path.join(output_dir, filename)
        plt.tight_layout()
        plt.savefig(filepath, bbox_inches='tight')
        plt.show()
    """


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

# Example usage: Plotting all metrics two at a time
plot_model_specific_comparison(metrics_dict)
