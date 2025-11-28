"""Visualization functions."""
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from wordcloud import WordCloud
from typing import List
from pathlib import Path


def visualize_word_cloud(
    X_train: pd.DataFrame, 
    y_train: pd.DataFrame, 
    token: str, 
    max_words: int = 50,
    output_dir: str = '.'
) -> List[str]:
    """
    Visualize the most common words contributing to the token and return top words.

    Parameters:
    - X_train (pd.DataFrame): Training set features.
    - y_train (pd.DataFrame): Training set labels.
    - token (str): The label to visualize word cloud for.
    - max_words (int): Number of top words to return.

    Returns:
    - top_words (list): List of top words based on frequency.
    """
    description_context = X_train.join(y_train)
    description_context = description_context[description_context[token] == 1]
    if description_context.empty:
        print(f"No instances for label '{token}'; skipping word cloud.")
        return []

    description_text = description_context['report']
    combined_text = ' '.join(description_text)
    wordcloud = WordCloud(width=1600, height=800, max_font_size=200, background_color='white').generate(combined_text)
    plt.figure(figsize=(15, 10))
    plt.imshow(wordcloud.recolor(colormap="Blues"), interpolation='bilinear')
    plt.axis("off")
    plt.title(f"Most Common Words Associated with '{token}' Defects", size=20)
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/wordcloud_{token}.png')
    plt.close()
    print(f"Word cloud for '{token}' saved as '{output_dir}/wordcloud_{token}.png'.")

    word_freq = wordcloud.words_
    sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)
    top_words = [word for word, freq in sorted_words[:max_words]]
    return top_words


def visualize_description_length(X_train: pd.DataFrame, data_type: str, output_dir: str = '.'):
    """Visualize the distribution of description lengths."""
    sns.set(style="darkgrid")
    X_train['report'] = X_train['report'].astype(str)
    description_len = X_train['report'].str.len()
    plt.figure(figsize=(10, 6))
    sns.histplot(description_len, kde=False, bins=20, color="steelblue")
    plt.xlabel('Description Length')
    plt.ylabel('Frequency')
    plt.title('Description Length Distribution')
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/description_length_distribution_{data_type}.png')
    plt.close()
    print(f"Description length distribution plot saved at {output_dir}.")


def visualize_class_distribution(y_train: pd.DataFrame, y_test: pd.DataFrame, data_type: str, output_dir: str = '.'):
    """Visualize the distribution of classes within each label."""
    labels = y_train.columns.tolist()
    bar_width = 0.2
    bars1 = [sum(y_train[label] == 1) for label in labels]
    bars2 = [sum(y_train[label] == 0) for label in labels]
    bars3 = [sum(y_test[label] == 1) for label in labels]
    bars4 = [sum(y_test[label] == 0) for label in labels]

    r1 = np.arange(len(bars1))
    r2 = [x + bar_width for x in r1]
    r3 = [x + bar_width for x in r2]
    r4 = [x + bar_width for x in r3]

    plt.figure(figsize=(12, 8))
    plt.bar(r1, bars1, color='steelblue', width=bar_width, label='Train Labeled = 1')
    plt.bar(r2, bars2, color='lightsteelblue', width=bar_width, label='Train Labeled = 0')
    plt.bar(r3, bars3, color='darkorange', width=bar_width, label='Test Labeled = 1')
    plt.bar(r4, bars4, color='navajowhite', width=bar_width, label='Test Labeled = 0')

    plt.xlabel('Labels', fontweight='bold')
    plt.xticks([r + bar_width * 1.5 for r in range(len(bars1))], labels, rotation=45)
    plt.legend()
    plt.title('Distribution of Classes within Each Label')
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/{data_type}_class_distribution.png')
    plt.close()
    print(f"Class distribution plot saved at {output_dir}.")


def visualize_correlation_matrix(y_train: pd.DataFrame, data_type: str, output_dir: str = '.'):
    """Visualize the cross-correlation matrix across labels."""
    if y_train.empty:
        print("No data available for correlation matrix; skipping plot.")
        return

    corr = y_train.corr()
    plt.figure(figsize=(10, 8))
    sns.heatmap(corr, annot=True, cmap="Blues", fmt=".2f",
                xticklabels=corr.columns.values, yticklabels=corr.columns.values)
    plt.title('Correlation Matrix of Labels')
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/label_correlation_matrix_{data_type}.png')
    plt.close()
    print(f"Label correlation matrix plot saved at {output_dir}.")


def visualize_f1_scores(methods: pd.DataFrame, data_type: str, output_dir: str = '.'):
    """Visualize F1 score results through a box plot."""
    plt.figure(figsize=(12, 8))
    ax = sns.boxplot(x='Model', y='F1', data=methods, palette="Blues")
    sns.stripplot(x='Model', y='F1', data=methods, size=8, jitter=True,
                  edgecolor="gray", linewidth=2, palette="Blues")
    ax.set_xticklabels(ax.get_xticklabels(), rotation=20)
    plt.title('F1 Score Distribution by Model')
    plt.ylabel('F1 Score')
    plt.xlabel('Model')
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/f1_score_distribution_{data_type}.png')
    plt.close()
    print(f"F1 score distribution plot saved at {output_dir}.")


def plot_top_features(selected_features: np.ndarray, chi2_scores_max: np.ndarray, 
                     data_type: str, top_k_plot: int = 20, output_dir: str = '.'):
    """Plot the top features based on Chi-Square scores."""
    top_features = selected_features[:top_k_plot]
    selected_indices = np.argsort(chi2_scores_max)[::-1][:top_k_plot]
    selected_indices = selected_indices.astype(int)
    top_scores = chi2_scores_max[selected_indices[:top_k_plot]]
    
    plt.figure(figsize=(12, 8))
    sns.barplot(x=top_scores, y=top_features)
    plt.title(f'Top {top_k_plot} Features Based on Chi-Square Scores')
    plt.xlabel('Chi-Square Score')
    plt.ylabel('Feature')
    plt.tight_layout()
    import os
    os.makedirs(output_dir, exist_ok=True)
    plt.savefig(f'{output_dir}/chi2_features_{data_type}.png')
    plt.close()
    print(f"Top {top_k_plot} features plotted at {output_dir}.")


def generate_performance_charts(df_results: pd.DataFrame, data_type: str, output_dir: str = '.') -> List[str]:
    """
    Generate comparison visuals for Recall, F1, and Hamming Loss across models/labels.
    Returns list of generated chart paths.
    """
    if df_results is None or df_results.empty:
        return []

    charts = []
    out_dir = Path(output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    # Heatmap of F1 by Label/Model
    try:
        f1_pivot = df_results.pivot_table(index='Label', columns='Model', values='F1')
        plt.figure(figsize=(10, 6))
        sns.heatmap(f1_pivot, annot=True, cmap="Blues", fmt=".3f")
        plt.title(f'F1 Heatmap ({data_type})')
        plt.tight_layout()
        path = out_dir / f"f1_heatmap_{data_type.lower()}.png"
        plt.savefig(path)
        plt.close()
        charts.append(str(path))
    except Exception as e:
        print(f"[WARN] Skipping F1 heatmap: {e}")

    # Heatmap of Recall by Label/Model
    try:
        recall_pivot = df_results.pivot_table(index='Label', columns='Model', values='Recall')
        plt.figure(figsize=(10, 6))
        sns.heatmap(recall_pivot, annot=True, cmap="Greens", fmt=".3f")
        plt.title(f'Recall Heatmap ({data_type})')
        plt.tight_layout()
        path = out_dir / f"recall_heatmap_{data_type.lower()}.png"
        plt.savefig(path)
        plt.close()
        charts.append(str(path))
    except Exception as e:
        print(f"[WARN] Skipping Recall heatmap: {e}")

    # Bar chart of average Hamming Loss by model
    try:
        loss_summary = df_results.groupby('Model')['Hamming Loss'].mean().reset_index()
        plt.figure(figsize=(8, 5))
        sns.barplot(x='Model', y='Hamming Loss', data=loss_summary, palette="Reds")
        plt.title(f'Hamming Loss by Model ({data_type})')
        plt.ylim(0, loss_summary['Hamming Loss'].max() * 1.2 if not loss_summary.empty else 1)
        plt.xticks(rotation=20)
        plt.tight_layout()
        path = out_dir / f"hamming_loss_by_model_{data_type.lower()}.png"
        plt.savefig(path)
        plt.close()
        charts.append(str(path))
    except Exception as e:
        print(f"[WARN] Skipping Hamming Loss chart: {e}")

    # Scatter of Recall vs F1 per model/label
    try:
        plt.figure(figsize=(8, 6))
        sns.scatterplot(x='Recall', y='F1', hue='Model', style='Label', data=df_results)
        plt.title(f'Recall vs F1 ({data_type})')
        plt.xlim(0, 1)
        plt.ylim(0, 1)
        plt.tight_layout()
        path = out_dir / f"recall_vs_f1_{data_type.lower()}.png"
        plt.savefig(path)
        plt.close()
        charts.append(str(path))
    except Exception as e:
        print(f"[WARN] Skipping Recall vs F1 scatter: {e}")

    return charts
