"""
Configurable multi-class classification pipeline for software change type prediction.

This script mirrors the existing notebook-style logic but is driven by a JSON config.
It produces per-run artifacts under output/reports/<run_name>_<timestamp> alongside
an HTML report and metadata for the Java UI.
"""

import argparse
import json
import logging
import warnings
from datetime import datetime
from pathlib import Path

import matplotlib.pyplot as plt
import nltk
import numpy as np
import pandas as pd
import seaborn as sns
import torch
from imblearn.over_sampling import SMOTE
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    precision_recall_fscore_support,
)
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.preprocessing import LabelEncoder
from sklearn.svm import LinearSVC
from sklearn.utils import resample
from torch.utils.data import DataLoader, RandomSampler, SequentialSampler, TensorDataset
from transformers import AdamW, BertForSequenceClassification, BertTokenizer, get_linear_schedule_with_warmup
from wordcloud import WordCloud

warnings.filterwarnings("ignore")
nltk.download("punkt", quiet=True)
nltk.download("stopwords", quiet=True)
nltk.download("wordnet", quiet=True)


def load_config(path: Path) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def setup_logging(run_folder: Path) -> logging.Logger:
    logger = logging.getLogger("multiclass")
    logger.setLevel(logging.INFO)
    logger.handlers.clear()
    formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")

    fh = logging.FileHandler(run_folder / "run.log")
    fh.setFormatter(formatter)
    logger.addHandler(fh)

    sh = logging.StreamHandler()
    sh.setFormatter(formatter)
    logger.addHandler(sh)
    return logger


def preprocess_text(text, lowercase=True, remove_non_alpha=True, remove_stopwords=True, lemmatize=True):
    from nltk.corpus import stopwords
    from nltk.stem import WordNetLemmatizer
    import re

    if lowercase:
        text = text.lower()
    if remove_non_alpha:
        text = re.sub(r"[^a-z\s]", " ", text)
    tokens = nltk.word_tokenize(text)
    if remove_stopwords:
        tokens = [t for t in tokens if t not in set(stopwords.words("english"))]
    if lemmatize:
        lemmatizer = WordNetLemmatizer()
        tokens = [lemmatizer.lemmatize(t) for t in tokens]
    return " ".join(tokens)


def plot_class_distribution(df, label_col, output_path: Path, title: str):
    plt.figure(figsize=(10, 6))
    sns.countplot(x=label_col, data=df, order=df[label_col].value_counts().index)
    plt.xticks(rotation=45)
    plt.title(title)
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()


def plot_train_distribution(labels, class_names, output_path: Path, title: str):
    plt.figure(figsize=(12, 5))
    sns.countplot(x=labels)
    plt.title(title)
    plt.xlabel("Classes")
    plt.ylabel("Count")
    plt.xticks(ticks=range(len(class_names)), labels=class_names, rotation=45)
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()


def generate_wordclouds(data: pd.DataFrame, label_col: str, text_col: str, output_dir: Path):
    classes = data[label_col].unique()
    for cls in classes:
        text = " ".join(data[data[label_col] == cls][text_col])
        wordcloud = WordCloud(width=800, height=400, background_color="white").generate(text)
        plt.figure(figsize=(10, 5))
        plt.imshow(wordcloud, interpolation="bilinear")
        plt.axis("off")
        plt.title(f"Word Cloud for {cls}")
        plt.tight_layout()
        plt.savefig(output_dir / f"wordcloud_{cls}.png")
        plt.close()


def collect_metrics(model_name, model, X_train, y_train, X_test, y_test, labels):
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    report = classification_report(y_test, y_pred, target_names=labels, output_dict=True, zero_division=0)
    accuracy = accuracy_score(y_test, y_pred)
    precision, recall, f1, _ = precision_recall_fscore_support(y_test, y_pred, average="weighted", zero_division=0)
    return {
        "Model": model_name,
        "Accuracy": accuracy,
        "Precision": precision,
        "Recall": recall,
        "F1-Score": f1,
        "report": report,
    }


def plot_metrics(metrics_df: pd.DataFrame, data_desc: str, output_dir: Path, charts: list, visual_flags: dict):
    if metrics_df.empty:
        return
    if visual_flags.get("metrics_boxplot", True):
        plt.figure(figsize=(10, 6))
        sns.boxplot(data=metrics_df[["Accuracy", "Precision", "Recall", "F1-Score"]], palette="Set3")
        plt.title(f"Evaluation Metrics - Box Plot - {data_desc}")
        plt.ylabel("Score")
        plt.xlabel("Metrics")
        plt.tight_layout()
        box_path = output_dir / f"metrics_box_{data_desc}.png"
        plt.savefig(box_path)
        plt.close()
        charts.append(box_path.name)

    if visual_flags.get("metrics_barplot", True):
        metrics_df.set_index("Model")[["Accuracy", "Precision", "Recall", "F1-Score"]].plot(
            kind="bar", figsize=(10, 6), colormap="Set2"
        )
        plt.title(f"Evaluation Metrics - Bar Plot - {data_desc}")
        plt.ylabel("Score")
        plt.xlabel("Model")
        plt.xticks(rotation=0)
        plt.tight_layout()
        bar_path = output_dir / f"metrics_bar_{data_desc}.png"
        plt.savefig(bar_path)
        plt.close()
        charts.append(bar_path.name)


def tokenize_texts(tokenizer, texts, max_length: int):
    return tokenizer(
        texts,
        add_special_tokens=True,
        max_length=max_length,
        padding="max_length",
        truncation=True,
        return_attention_mask=True,
        return_tensors="pt",
    )


def train_evaluate_bert(train_texts, train_labels, test_texts, test_labels, label_names, bert_conf, device, output_dir, charts, data_label):
    tokenizer = BertTokenizer.from_pretrained("bert-base-uncased")
    train_enc = tokenize_texts(tokenizer, train_texts, bert_conf["max_length"])
    test_enc = tokenize_texts(tokenizer, test_texts, bert_conf["max_length"])

    train_dataset = TensorDataset(train_enc["input_ids"], train_enc["attention_mask"], torch.tensor(train_labels.tolist()))
    test_dataset = TensorDataset(test_enc["input_ids"], test_enc["attention_mask"], torch.tensor(test_labels.tolist()))

    train_loader = DataLoader(train_dataset, sampler=RandomSampler(train_dataset), batch_size=bert_conf["batch_size"])
    test_loader = DataLoader(test_dataset, sampler=SequentialSampler(test_dataset), batch_size=bert_conf["batch_size"])

    model = BertForSequenceClassification.from_pretrained("bert-base-uncased", num_labels=len(label_names))
    model.to(device)

    optim_choice = bert_conf.get("optimizer", "adamw").lower()
    if optim_choice == "adam":
        optimizer = torch.optim.Adam(model.parameters(), lr=bert_conf["learning_rate"])
    elif optim_choice == "pso":
        logging.getLogger("multiclass").warning("PSO optimizer not implemented for BERT; falling back to AdamW.")
        optimizer = AdamW(model.parameters(), lr=bert_conf["learning_rate"], eps=1e-8)
    else:
        optimizer = AdamW(model.parameters(), lr=bert_conf["learning_rate"], eps=1e-8)
    total_steps = len(train_loader) * bert_conf["epochs"]
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=0, num_training_steps=total_steps)

    def train_epoch():
        model.train()
        total_loss = 0.0
        for batch in train_loader:
            optimizer.zero_grad()
            input_ids, attention_mask, labels = [b.to(device) for b in batch]
            outputs = model(input_ids, attention_mask=attention_mask, labels=labels)
            loss = outputs.loss
            total_loss += loss.item()
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
            optimizer.step()
            scheduler.step()
        return total_loss / max(1, len(train_loader))

    def evaluate():
        model.eval()
        preds, gold = [], []
        with torch.no_grad():
            for batch in test_loader:
                input_ids, attention_mask, labels = [b.to(device) for b in batch]
                outputs = model(input_ids, attention_mask=attention_mask)
                logits = outputs.logits
                preds.extend(torch.argmax(logits, axis=1).cpu().numpy())
                gold.extend(labels.cpu().numpy())
        report = classification_report(gold, preds, target_names=label_names, output_dict=True, zero_division=0)
        acc = accuracy_score(gold, preds)
        precision, recall, f1, _ = precision_recall_fscore_support(gold, preds, average="weighted", zero_division=0)
        return {
            "Model": "BERT",
            "Accuracy": acc,
            "Precision": precision,
            "Recall": recall,
            "F1-Score": f1,
            "report": report,
        }

    for epoch in range(bert_conf["epochs"]):
        train_epoch()
    metrics = evaluate()
    plot_metrics(pd.DataFrame([metrics]), data_label, output_dir, charts, {"metrics_boxplot": True, "metrics_barplot": True})
    return metrics


def build_report(run_folder: Path, run_name: str, timestamp: str, data_results: dict, charts: list, hyperparams: dict):
    html_path = run_folder / "report.html"
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(f"<html><head><title>{run_name} ({timestamp})</title></head><body>")
        f.write(f"<h1>{run_name}</h1>")
        f.write(f"<p>Timestamp: {timestamp}</p>")
        f.write("<h2>Metrics</h2>")
        for data_type, df in data_results.items():
            f.write(f"<h3>{data_type}</h3>")
            if df is None or df.empty:
                f.write("<p>No results produced.</p>")
                continue
            f.write(df.to_html(index=False, float_format="{:.4f}".format))
        if hyperparams:
            f.write("<h2>Hyperparameters</h2>")
            for data_type, params in hyperparams.items():
                f.write(f"<h4>{data_type}</h4><ul>")
                for entry in params:
                    model = entry.get("model", "?")
                    family = entry.get("family", "?")
                    details = ", ".join(f"{k}={v}" for k, v in entry.get("parameters", {}).items())
                    f.write(f"<li><strong>{model}</strong> ({family}) - {details}</li>")
                f.write("</ul>")
        if charts:
            f.write("<h2>Charts</h2>")
            for chart in charts:
                f.write(f"<div><img src='{chart}' style='max-width:600px;'><br>{chart}</div>")
        f.write("</body></html>")
    return html_path


def write_metadata(run_folder: Path, run_name: str, timestamp: str, data_types, models, config_path, hyperparams, charts):
    meta = {
        "run_name": run_name,
        "timestamp": timestamp,
        "data_types": sorted(data_types),
        "models": sorted(models),
        "config_file": str(config_path),
        "status": "complete",
        "hyperparameters": hyperparams,
        "charts": charts,
    }
    with open(run_folder / "metadata.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)


def main(config_path: Path):
    config = load_config(config_path)
    experiment_name = config.get("experiment_name", "Multiclass Run")
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    reports_dir = Path(config.get("output", {}).get("reports_dir", "output/reports"))
    if not reports_dir.is_absolute():
        reports_dir = config_path.parent / reports_dir
    run_folder = reports_dir / f"{experiment_name}_{timestamp}"
    run_folder.mkdir(parents=True, exist_ok=True)

    logger = setup_logging(run_folder)
    logger.info("Starting run: %s", experiment_name)

    data_cfg = config.get("data", {})
    fe_cfg = config.get("feature_engineering", {})
    preprocess_cfg = config.get("preprocessing", {})
    visual_cfg = config.get("visualizations", {})
    models_cfg = config.get("models", {})

    dataset_path = Path(data_cfg.get("dataset_path", "dataset.csv"))
    if not dataset_path.is_absolute():
        dataset_path = config_path.parent / dataset_path
    if not dataset_path.exists():
        logger.error("Dataset not found at %s", dataset_path)
        return 1

    df = pd.read_csv(dataset_path)
    text_col = data_cfg.get("text_column", "report")
    label_col = data_cfg.get("label_column", "target")

    df = df[[text_col, label_col]].dropna()
    logger.info("Loaded dataset with %d rows", len(df))

    df["cleaned_text"] = df[text_col].apply(
        lambda x: preprocess_text(
            str(x),
            lowercase=preprocess_cfg.get("lowercase", True),
            remove_non_alpha=preprocess_cfg.get("remove_non_alpha", True),
            remove_stopwords=preprocess_cfg.get("remove_stopwords", True),
            lemmatize=preprocess_cfg.get("lemmatize", True),
        )
    )

    label_encoder = LabelEncoder()
    df["label_encoded"] = label_encoder.fit_transform(df[label_col])
    class_names = label_encoder.classes_

    charts = []

    if visual_cfg.get("class_distribution", True):
        plot_class_distribution(df, label_col, run_folder / "class_distribution.png", "Class Distribution")
        charts.append("class_distribution.png")

    if visual_cfg.get("word_clouds", True):
        generate_wordclouds(df, label_col, "cleaned_text", run_folder)
        charts.extend([p.name for p in run_folder.glob("wordcloud_*.png")])

    test_size = data_cfg.get("test_size", 0.2)
    random_state = data_cfg.get("random_state", 42)
    X_train_raw, X_test_raw, y_train, y_test = train_test_split(
        df["cleaned_text"], df["label_encoded"], test_size=test_size, random_state=random_state, stratify=df["label_encoded"]
    )

    if visual_cfg.get("train_distribution", True):
        plot_train_distribution(y_train, class_names, run_folder / "train_dist_unbalanced.png", "Train Data (Unbalanced)")
        charts.append("train_dist_unbalanced.png")

    vectorizer_choice = str(fe_cfg.get("vectorizer", "tfidf")).lower()
    use_bert_vectorizer = vectorizer_choice == "bert_tokenizer"

    X_train_vec = None
    X_test_vec = None
    smote_cfg = data_cfg.get("smote", {"enabled": True, "k_neighbors": 5})
    X_train_balanced, y_train_balanced = None, None
    balanced_available = False

    if use_bert_vectorizer:
        logger.info("Vectorizer set to BERT tokenizer. Traditional models will be skipped; only BERT will run.")
        if not models_cfg.get("bert", {}).get("enabled", False):
            logger.warning("BERT tokenizer selected but BERT model disabled. Enable BERT or switch vectorizer back to tfidf.")
    else:
        vectorizer = TfidfVectorizer(
            max_features=preprocess_cfg.get("max_features", 5000),
            ngram_range=tuple(preprocess_cfg.get("ngram_range", (1, 1))),
        )
        X_train_vec = vectorizer.fit_transform(X_train_raw)
        X_test_vec = vectorizer.transform(X_test_raw)

        if data_cfg.get("run_balanced", True) and smote_cfg.get("enabled", True):
            sm = SMOTE(random_state=random_state, k_neighbors=smote_cfg.get("k_neighbors", 5))
            X_train_balanced, y_train_balanced = sm.fit_resample(X_train_vec, y_train)
            balanced_available = True
            if visual_cfg.get("train_distribution", True):
                plot_train_distribution(
                    y_train_balanced,
                    class_names,
                    run_folder / "train_dist_balanced.png",
                    "Train Data (Balanced)",
                )
                charts.append("train_dist_balanced.png")

    trad_models_cfg = models_cfg.get("traditional", {})
    enabled_models = []
    model_defs = []
    if trad_models_cfg.get("logistic_regression", {}).get("enabled", True):
        enabled_models.append("Logistic Regression")
        model_defs.append(("Logistic Regression", LogisticRegression(max_iter=trad_models_cfg.get("logistic_regression", {}).get("max_iter", 1000))))
    if trad_models_cfg.get("linear_svm", {}).get("enabled", True):
        enabled_models.append("Linear SVM")
        model_defs.append(("Linear SVM", LinearSVC()))
    if trad_models_cfg.get("multinomial_nb", {}).get("enabled", True):
        enabled_models.append("Multinomial NB")
        model_defs.append(("Multinomial NB", MultinomialNB()))
    if trad_models_cfg.get("random_forest", {}).get("enabled", True):
        rf_cfg = trad_models_cfg.get("random_forest", {})
        enabled_models.append("Random Forest")
        model_defs.append(("Random Forest", RandomForestClassifier(n_estimators=rf_cfg.get("n_estimators", 200), random_state=rf_cfg.get("random_state", 42))))

    results_by_type = {}
    hyperparams = {}
    models_used = set()

    def run_traditional(data_desc, Xtr, ytr):
        if not model_defs:
            return pd.DataFrame(), []
        metrics_rows = []
        params = []
        for name, model in model_defs:
            logger.info("Training %s on %s data", name, data_desc)
            metrics = collect_metrics(name, model, Xtr, ytr, X_test_vec, y_test, class_names)
            metrics_rows.append({k: v for k, v in metrics.items() if k != "report"})
            params.append(
                {
                    "model": name,
                    "family": "Traditional ML",
                    "parameters": trad_models_cfg.get(name.lower().replace(" ", "_"), {}),
                }
            )
            models_used.add(name)
        df_metrics = pd.DataFrame(metrics_rows)
        results_by_type[data_desc] = df_metrics
        hyperparams[data_desc] = params
        plot_metrics(df_metrics, data_desc.lower(), run_folder, charts, visual_cfg)
        return df_metrics

    if not use_bert_vectorizer and data_cfg.get("run_unbalanced", True):
        run_traditional("Unbalanced", X_train_vec, y_train)

    if not use_bert_vectorizer and balanced_available:
        run_traditional("Balanced", X_train_balanced, y_train_balanced)

    bert_cfg = models_cfg.get("bert", {"enabled": False})
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if bert_cfg.get("enabled", False):
        logger.info("Running BERT on unbalanced data")
        if visual_cfg.get("bert_distribution", True):
            plot_train_distribution(
                y_train,
                class_names,
                run_folder / "bert_train_dist_unbalanced.png",
                "BERT Train Data (Unbalanced)",
            )
            charts.append("bert_train_dist_unbalanced.png")
        bert_result_unbal = train_evaluate_bert(
            X_train_raw.tolist(),
            y_train,
            X_test_raw.tolist(),
            y_test,
            class_names,
            {
                "epochs": bert_cfg.get("epochs", 2),
                "batch_size": bert_cfg.get("batch_size", 8),
                "learning_rate": bert_cfg.get("learning_rate", 2e-5),
                "max_length": bert_cfg.get("max_length", 128),
                "optimizer": bert_cfg.get("optimizer", "adamw"),
            },
            device,
            run_folder,
            charts,
            "bert_unbalanced",
        )
        df_bert_unbal = pd.DataFrame([{k: v for k, v in bert_result_unbal.items() if k != "report"}])
        results_by_type.setdefault("Unbalanced", pd.DataFrame())
        results_by_type["Unbalanced"] = pd.concat([results_by_type["Unbalanced"], df_bert_unbal], ignore_index=True)
        hyperparams.setdefault("Unbalanced", []).append(
            {
                "model": "BERT",
                "family": "Deep Learning",
                "parameters": {
                    "epochs": bert_cfg.get("epochs", 2),
                    "batch_size": bert_cfg.get("batch_size", 8),
                    "learning_rate": bert_cfg.get("learning_rate", 2e-5),
                    "max_length": bert_cfg.get("max_length", 128),
                    "optimizer": bert_cfg.get("optimizer", "adamw"),
                },
            }
        )
        models_used.add("BERT")

        if data_cfg.get("run_balanced", True) and bert_cfg.get("balance_training", True):
            logger.info("Running BERT on balanced data (oversampled)")
            train_df = pd.DataFrame({"text": X_train_raw, "label": y_train})
            subsets = [train_df[train_df["label"] == i] for i in range(len(class_names))]
            max_size = max(len(s) for s in subsets)
            balanced_df = pd.concat(
                [
                    resample(s, replace=True, n_samples=max_size, random_state=random_state) if len(s) < max_size else s
                    for s in subsets
                ]
            ).sample(frac=1, random_state=random_state)
            if visual_cfg.get("bert_distribution", True):
                plot_train_distribution(
                    balanced_df["label"],
                    class_names,
                    run_folder / "bert_train_dist_balanced.png",
                    "BERT Train Data (Balanced/Oversampled)",
                )
                charts.append("bert_train_dist_balanced.png")
            bert_result_bal = train_evaluate_bert(
                balanced_df["text"].tolist(),
                balanced_df["label"],
                X_test_raw.tolist(),
                y_test,
                class_names,
                {
                    "epochs": bert_cfg.get("epochs", 2),
                    "batch_size": bert_cfg.get("batch_size", 8),
                    "learning_rate": bert_cfg.get("learning_rate", 2e-5),
                    "max_length": bert_cfg.get("max_length", 128),
                    "optimizer": bert_cfg.get("optimizer", "adamw"),
                },
                device,
                run_folder,
                charts,
                "bert_balanced",
            )
            df_bert_bal = pd.DataFrame([{k: v for k, v in bert_result_bal.items() if k != "report"}])
            results_by_type.setdefault("Balanced", pd.DataFrame())
            results_by_type["Balanced"] = pd.concat([results_by_type["Balanced"], df_bert_bal], ignore_index=True)
            hyperparams.setdefault("Balanced", []).append(
                {
                    "model": "BERT",
                    "family": "Deep Learning",
                    "parameters": {
                        "epochs": bert_cfg.get("epochs", 2),
                        "batch_size": bert_cfg.get("batch_size", 8),
                        "learning_rate": bert_cfg.get("learning_rate", 2e-5),
                        "max_length": bert_cfg.get("max_length", 128),
                        "optimizer": bert_cfg.get("optimizer", "adamw"),
                        "balanced": True,
                    },
                }
            )
            models_used.add("BERT")

    output_cfg = config.get("output", {})
    if output_cfg.get("save_results_csv", True):
        for data_type, df_res in results_by_type.items():
            if df_res is not None and not df_res.empty:
                name = output_cfg.get(
                    f"results_filename_{data_type.lower()}",
                    f"results_{data_type.lower()}.csv",
                )
                df_res.to_csv(run_folder / name, index=False)

    report_path = build_report(run_folder, experiment_name, timestamp, results_by_type, charts, hyperparams)
    write_metadata(run_folder, experiment_name, timestamp, results_by_type.keys(), models_used, config_path, hyperparams, charts)
    with open(run_folder / "COMPLETE.flag", "w", encoding="utf-8") as f:
        f.write("complete")

    logger.info("Run complete. Report written to %s", report_path)
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Configurable multi-class classifier.")
    parser.add_argument("config", nargs="?", default="../configs/quick_test_multiclass.json", help="Path to config JSON.")
    args = parser.parse_args()
    exit(main(Path(args.config).resolve()))
