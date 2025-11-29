package com.phd.ui;

import com.phd.config.ConfigManager;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPersistenceTest {

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
        // Initialize ConfigManager before tests
        ConfigManager.initialize();
    }

    @Test
    void featureSamplePanelPersistsMultiLabelConfig() throws Exception {
        Path tempConfig = Files.createTempFile("ml-config", ".json");
        Files.writeString(tempConfig, new JSONObject()
                .put("data", new JSONObject())
                .put("feature_engineering", new JSONObject().put("tfidf", new JSONObject()))
                .toString(2), StandardCharsets.UTF_8);

        // Note: Test now uses ConfigManager which initializes from default paths
        // This test verifies the panel methods work correctly
        FeatureSamplePanel panel = new FeatureSamplePanel();
        setSpinner(panel, "sampleSizeSpinner", 123);
        setSpinner(panel, "sampleRandomSpinner", 7);
        setSpinner(panel, "testSizeSpinner", 0.3d);
        setCheckBox(panel, "runBalancedBox", true);
        setCheckBox(panel, "runUnbalancedBox", false);
        setSpinner(panel, "targetCountSpinner", 900);

        setCheckBox(panel, "useFeatureSelectionBox", true);
        setSpinner(panel, "topKSpinner", 15);
        setSpinner(panel, "topKPlotSpinner", 9);
        setCheckBox(panel, "wordCloudVocabularyBox", false);
        setSpinner(panel, "maxWordsSpinner", 33);
        setSpinner(panel, "ngramMinSpinner", 2);
        setSpinner(panel, "ngramMaxSpinner", 3);
        setSpinner(panel, "minDfSpinner", 4);
        setCheckBox(panel, "useIdfBox", false);

        panel.persistUiToModel();
        panel.saveSilently();

        // Read from ConfigManager's multilabel config
        JSONObject saved = com.phd.config.ConfigManager.getMultiLabelConfig();
        JSONObject data = saved.getJSONObject("data");
        JSONObject fe = saved.getJSONObject("feature_engineering");
        JSONObject tfidf = fe.getJSONObject("tfidf");

        assertEquals(123, data.getInt("sample_size"));
        assertEquals(7, data.getInt("sample_random_state"));
        assertEquals(0.3d, data.getDouble("test_size"));
        assertTrue(data.getBoolean("run_balanced"));
        assertEquals(false, data.getBoolean("run_unbalanced"));
        assertEquals(900, data.getInt("balanced_target_count"));

        assertTrue(fe.getBoolean("use_feature_selection"));
        assertEquals(15, fe.getInt("top_k"));
        assertEquals(9, fe.getInt("top_k_plot"));
        assertEquals(false, fe.getBoolean("use_wordcloud_vocabulary"));
        assertEquals(33, fe.getInt("max_words_per_label"));
        assertEquals(2, tfidf.getJSONArray("ngram_range").getInt(0));
        assertEquals(3, tfidf.getJSONArray("ngram_range").getInt(1));
        assertEquals(4, tfidf.getInt("min_df"));
        assertEquals(false, tfidf.getBoolean("use_idf"));
    }

    @Test
    void visualizationPanelPersistsBothConfigs() throws Exception {
        Path mlConfig = Files.createTempFile("ml-visuals", ".json");
        Path mcConfig = Files.createTempFile("mc-visuals", ".json");
        Files.writeString(mlConfig, new JSONObject().toString(2), StandardCharsets.UTF_8);
        Files.writeString(mcConfig, new JSONObject().toString(2), StandardCharsets.UTF_8);

        // Note: Test now uses ConfigManager which initializes from default paths
        VisualizationPanel panel = new VisualizationPanel();
        Map<String, JCheckBox> mlChecks = getCheckMap(panel, "mlChecks");
        Map<String, JCheckBox> mcChecks = getCheckMap(panel, "mcChecks");

        mlChecks.values().forEach(box -> box.setSelected(false));
        mcChecks.values().forEach(box -> box.setSelected(true));

        panel.saveSilently();

        // Read from ConfigManager's configs
        JSONObject mlSaved = com.phd.config.ConfigManager.getMultiLabelConfig();
        JSONObject mcSaved = com.phd.config.ConfigManager.getMultiClassConfig();

        mlChecks.keySet().forEach(key -> assertEquals(false, mlSaved.getJSONObject("visualizations").getBoolean(key)));
        mcChecks.keySet().forEach(key -> assertEquals(true, mcSaved.getJSONObject("visualizations").getBoolean(key)));
    }

    @Test
    void aiTechniquePanelPersistsBothProblemTypes() throws Exception {
        Path mlConfig = Files.createTempFile("ml-ai", ".json");
        Path mcConfig = Files.createTempFile("mc-ai", ".json");
        Files.writeString(mlConfig, new JSONObject().put("models", new JSONObject()).put("data", new JSONObject()).toString(2), StandardCharsets.UTF_8);
        Files.writeString(mcConfig, new JSONObject().put("models", new JSONObject()).put("data", new JSONObject()).toString(2), StandardCharsets.UTF_8);

        // Note: Test now uses ConfigManager; constructor takes only script paths
        AITechniquePanel panel = new AITechniquePanel("ml_script", "mc_script", null, null);

        // Multi-label selections (tab 0)
        setSelectedTab(panel, 0);
        setCheckBox(panel, "mlRandomForestEnabledBox", true);
        setSpinner(panel, "mlRandomForestEstimatorsSpinner", 321);
        setSpinner(panel, "mlRandomForestRandomStateSpinner", 99);
        setCheckBox(panel, "mlRandomForestChainBox", false);
        setCheckBox(panel, "mlRandomForestCvBox", true);

        setCheckBox(panel, "mlLogisticEnabledBox", true);
        setSpinner(panel, "mlLogisticMaxIterSpinner", 5000);

        setCheckBox(panel, "mlMultinomialEnabledBox", true);
        setCheckBox(panel, "mlMultinomialChainBox", true);
        setCheckBox(panel, "mlMultinomialCvBox", false);

        setCheckBox(panel, "mlRunBalancedBox", true);
        setCheckBox(panel, "mlRunUnbalancedBox", false);
        setSpinner(panel, "mlBalancedTargetSpinner", 777);

        setCheckBox(panel, "mlDeepLearningEnabledBox", true);
        setCheckBox(panel, "mlMlpEnabledBox", true);
        setSpinner(panel, "mlMlpEpochsSpinner", 11);
        setSpinner(panel, "mlMlpBatchSpinner", 12);
        setSpinner(panel, "mlMlpValidationSpinner", 0.15d);
        setSpinner(panel, "mlMlpLayer1UnitsSpinner", 128);
        setSpinner(panel, "mlMlpLayer2UnitsSpinner", 64);

        setCheckBox(panel, "mlCnnEnabledBox", true);
        setSpinner(panel, "mlCnnEpochsSpinner", 8);
        setSpinner(panel, "mlCnnBatchSpinner", 13);
        setSpinner(panel, "mlCnnMaxWordsSpinner", 4000);

        panel.persistInputsToModelForTest();
        // Config is now in ConfigManager, no file write needed
        
        JSONObject mlSaved = panel.getMultiLabelConfigForTest();
        JSONObject mlModels = mlSaved.getJSONObject("models").getJSONObject("traditional_ml");
        assertEquals(321, mlModels.getJSONObject("random_forest").getInt("n_estimators"));
        assertEquals(false, mlModels.getJSONObject("random_forest").getBoolean("use_classifier_chain"));
        assertEquals(5000, mlModels.getJSONObject("logistic_regression").getInt("max_iter"));
        assertEquals(true, mlModels.getJSONObject("multinomial_nb").getBoolean("enabled"));
        assertEquals(false, mlSaved.getJSONObject("data").getBoolean("run_unbalanced"));
        assertEquals(777, mlSaved.getJSONObject("data").getInt("balanced_target_count"));
        assertEquals(true, mlSaved.getJSONObject("models").getJSONObject("deep_learning").getBoolean("enabled"));

        // Multi-class selections (tab 1)
        setSelectedTab(panel, 1);
        setCheckBox(panel, "mcRunBalancedBox", false);
        setCheckBox(panel, "mcRunUnbalancedBox", true);
        setSpinner(panel, "mcSmoteNeighborsSpinner", 3);
        setSpinner(panel, "mcTfidfMaxFeaturesSpinner", 1234);
        setCheckBox(panel, "mcLogisticEnabledBox", true);
        setSpinner(panel, "mcMaxIterSpinner", 1500);
        setCheckBox(panel, "mcRandomForestEnabledBox", true);
        setSpinner(panel, "mcEstimatorsSpinner", 222);
        setCheckBox(panel, "mcLinearSvmEnabledBox", false);
        setCheckBox(panel, "mcMultinomialEnabledBox", false);
        setCheckBox(panel, "mcBertEnabledBox", true);
        setSpinner(panel, "mcBertEpochsSpinner", 6);
        setSpinner(panel, "mcBertBatchSpinner", 10);
        setSpinner(panel, "mcBertMaxLenSpinner", 64);
        setSpinner(panel, "mcBertLrSpinner", 0.0005d);

        panel.persistInputsToModelForTest();
        // Config is now in ConfigManager, no file write needed
        
        JSONObject mcSaved = panel.getMultiClassConfigForTest();
        JSONObject mcData = mcSaved.getJSONObject("data");
        assertEquals(true, mcData.getBoolean("run_unbalanced"));
        assertEquals(false, mcData.getBoolean("run_balanced"));
        assertEquals(3, mcData.getJSONObject("smote").getInt("k_neighbors"));
        JSONObject mcTraditional = mcSaved.getJSONObject("models").getJSONObject("traditional");
        assertEquals(1500, mcTraditional.getJSONObject("logistic_regression").getInt("max_iter"));
        assertEquals(222, mcTraditional.getJSONObject("random_forest").getInt("n_estimators"));
        JSONObject mcBert = mcSaved.getJSONObject("models").getJSONObject("bert");
        assertEquals(true, mcBert.getBoolean("enabled"));
        assertEquals(6, mcBert.getInt("epochs"));
        assertEquals(10, mcBert.getInt("batch_size"));
        assertEquals(64, mcBert.getInt("max_length"));
    }

    // Reflection helpers
    @SuppressWarnings("unchecked")
    private Map<String, JCheckBox> getCheckMap(Object instance, String fieldName) throws Exception {
        Field f = instance.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return (Map<String, JCheckBox>) f.get(instance);
    }

    private void setSpinner(Object instance, String fieldName, Number value) throws Exception {
        Field f = instance.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        JSpinner spinner = (JSpinner) f.get(instance);
        spinner.setValue(value);
    }

    private void setCheckBox(Object instance, String fieldName, boolean value) throws Exception {
        Field f = instance.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        JCheckBox box = (JCheckBox) f.get(instance);
        box.setSelected(value);
    }

    private void setSelectedTab(AITechniquePanel panel, int index) throws Exception {
        Field f = AITechniquePanel.class.getDeclaredField("problemTabs");
        f.setAccessible(true);
        JTabbedPane tabs = (JTabbedPane) f.get(panel);
        tabs.setSelectedIndex(index);
    }
}
