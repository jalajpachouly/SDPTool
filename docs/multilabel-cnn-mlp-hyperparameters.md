# Multi-label Deep Learning Hyperparameters (CNN & MLP)

This summarizes the CNN and MLP controls in the UI and how they affect training/evaluation.

## Shared toggles
- **Run cross-validation**: When enabled, trains/evaluates the model with K-fold CV using the CV-specific settings below; otherwise trains once on the full training split using the main `epochs`/`batch size`.
- **CV folds** (`cv_n_splits`): Number of folds (K) for cross-validation.
- **CV epochs** (`cv_epochs`): Training epochs for each CV fold.
- **CV batch size** (`cv_batch_size`): Mini-batch size used during CV.
- **Epochs** (`epochs`): Training epochs for the final fit on the full training split (after CV).
- **Batch size** (`batch_size`): Mini-batch size for the final fit.
- **Validation split** (`validation_split`): Fraction of the training data held out for validation during the final fit.
- **Early stopping patience** (`early_stopping_patience`): Number of epochs with no validation loss improvement before training stops (both CV and final fit).

## CNN-specific
- **Run cross-validation**: Same behavior as above, but for the CNN.
- **Max words** (`max_words`): Vocabulary size limit for the tokenizer (top N most frequent tokens).
- **Sequence length / Max len** (`max_len`): Fixed input sequence length; shorter sequences are padded, longer are truncated.
- **Embedding size** (`embedding_dim`): Dimension of the embedding vectors.
- **Conv filters** (`conv_filters`): Number of filters in the convolutional layer.
- **Kernel size** (`conv_kernel_size`): Width of the convolutional kernel (e.g., 3, 5).
- **Dense units** (`dense_units`): Units in the dense layer after convolution/pooling.
- **Dropout** (`dropout`): Dropout rate applied in the CNN to reduce overfitting.

## MLP-specific
- **Enable MLP**: Toggles the MLP model on/off.
- **Run cross-validation**: Same behavior as above, but for the MLP.
- **CV folds / CV epochs / CV batch size / Epochs / Batch size / Validation split / Early stopping patience**: As described in Shared toggles.
- **Layer 1 units** (`layer1_units`): Neurons in the first hidden layer.
- **Layer 1 dropout** (`layer1_dropout`): Dropout rate after the first hidden layer.
- **Layer 2 units** (`layer2_units`): Neurons in the second hidden layer.
- **Layer 2 dropout** (`layer2_dropout`): Dropout rate after the second hidden layer.
