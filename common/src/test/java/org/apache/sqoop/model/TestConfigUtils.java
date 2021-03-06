/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.model;

import com.google.common.base.Strings;
import org.apache.sqoop.validation.Status;
import org.apache.sqoop.validation.validators.AbstractValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sqoop.common.SqoopException;
import org.joda.time.DateTime;

/**
 * Test config utils
 */
public class TestConfigUtils {

  @Test
  public void testConfigs() {
    TestConfiguration config = new TestConfiguration();
    config.aConfig.a1 = "value";
    config.cConfig.enumeration = Enumeration.X;

    List<MValidator> expectedValidatorsOnAConfig = new ArrayList<>();
    expectedValidatorsOnAConfig.add(new MValidator(AConfig.AConfigValidator.class.getName(), AbstractValidator.DEFAULT_STRING_ARGUMENT));

    List<MValidator> expectedValidatorsOnA1 = new ArrayList<>();
    expectedValidatorsOnA1.add(new MValidator(AConfig.A1Validator.class.getName(), AbstractValidator.DEFAULT_STRING_ARGUMENT));

    List<MConfig> configsByInstance = ConfigUtils.toConfigs(config);
    assertEquals(getConfigs(), configsByInstance);
    assertEquals("value", configsByInstance.get(0).getInputs().get(0).getValue());
    assertEquals(expectedValidatorsOnA1, configsByInstance.get(0).getInputs().get(0).getValidators());
    assertEquals("X", configsByInstance.get(2).getInput("cConfig.enumeration").getValue());
    assertEquals(expectedValidatorsOnAConfig, configsByInstance.get(0).getValidators());

    List<MConfig> configsByClass = ConfigUtils.toConfigs(TestConfiguration.class);
    assertEquals(getConfigs(), configsByClass);

    List<MConfig> configsByBoth = ConfigUtils.toConfigs(TestConfiguration.class, config);
    assertEquals(getConfigs(), configsByBoth);
    assertEquals("value", configsByBoth.get(0).getInputs().get(0).getValue());
    assertEquals("X", configsByBoth.get(2).getInput("cConfig.enumeration").getValue());
  }

  @Test(expectedExceptions = SqoopException.class)
  public void testBadConfigInputsWithNonExisitingOverride() {
    TestBadConfiguration config = new TestBadConfiguration();
    config.aBadConfig.a1 = "value";
    ConfigUtils.toConfigs(config);
  }

  @Test(expectedExceptions = SqoopException.class)
  public void testBadConfigInputsWithBadOverride() {
    TestBadConfiguration1 config = new TestBadConfiguration1();
    config.aBadConfig1.a1 = "value";
    ConfigUtils.toConfigs(config);
  }

  @Test(expectedExceptions = SqoopException.class)
  public void testBadConfigInputsWithSelfOverride() {
    TestBadConfiguration2 config = new TestBadConfiguration2();
    config.aBadConfig2.a1 = "value";
    ConfigUtils.toConfigs(config);
  }

  @Test
  public void testConfigsMissingAnnotation() {
    try {
      ConfigUtils.toConfigs(ConfigWithoutAnnotation.class);
    } catch (SqoopException ex) {
      assertEquals(ModelError.MODEL_003, ex.getErrorCode());
      return;
    }

    Assert.fail("Correct exception wasn't thrown");
  }

  @Test
  public void testNonUniqueConfigNameAttributes() {
    try {
      ConfigUtils.toConfigs(ConfigurationWithNonUniqueConfigNameAttribute.class);
    } catch (SqoopException ex) {
      assertEquals(ModelError.MODEL_012, ex.getErrorCode());
      return;
    }

    Assert.fail("Correct exception wasn't thrown");
  }

  @Test
  public void testInvalidConfigNameAttribute() {
    try {
      ConfigUtils.toConfigs(ConfigurationWithInvalidConfigNameAttribute.class);
    } catch (SqoopException ex) {
      assertEquals(ModelError.MODEL_013, ex.getErrorCode());
      return;
    }
    Assert.fail("Correct exception wasn't thrown");
  }

  @Test
  public void testInvalidConfigNameAttributeLength() {
    try {
      ConfigUtils.toConfigs(ConfigurationWithInvalidConfigNameAttributeLength.class);
    } catch (SqoopException ex) {
      assertEquals(ModelError.MODEL_014, ex.getErrorCode());
      return;
    }
    Assert.fail("Correct exception wasn't thrown");
  }

  @Test
  public void testFailureOnPrimitiveType() {
    PrimitiveConfig config = new PrimitiveConfig();

    try {
      ConfigUtils.toConfigs(config);
      Assert.fail("We were expecting exception for unsupported type.");
    } catch (SqoopException ex) {
      assertEquals(ModelError.MODEL_007, ex.getErrorCode());
    }
  }

  @Test
  public void testFillValues() {
    List<MConfig> configs = getConfigs();

    ((MStringInput) configs.get(0).getInputs().get(0)).setValue("value");

    TestConfiguration config = new TestConfiguration();

    ConfigUtils.fromConfigs(configs, config);
    assertEquals("value", config.aConfig.a1);
  }

  @Test
  public void testFromConfigWithClass() {
    List<MConfig> configs = getConfigs();

    ((MStringInput) configs.get(0).getInputs().get(0)).setValue("value");

    TestConfiguration config = (TestConfiguration) ConfigUtils.fromConfigs(configs,
            TestConfiguration.class);
    assertEquals("value", config.aConfig.a1);
  }

  @Test
  public void testFillValuesObjectReuse() {
    List<MConfig> configs = getConfigs();

    ((MStringInput) configs.get(0).getInputs().get(0)).setValue("value");

    TestConfiguration config = new TestConfiguration();
    config.aConfig.a2 = "x";
    config.bConfig.b1 = "y";

    ConfigUtils.fromConfigs(configs, config);
    assertEquals("value", config.aConfig.a1);
    assertNull(config.aConfig.a2);
    assertNull(config.bConfig.b2);
    assertNull(config.bConfig.b2);
  }

  @Test
  public void testJson() {
    TestConfiguration config = new TestConfiguration();
    config.aConfig.a1 = "A";
    config.bConfig.b2 = "B";
    config.cConfig.longValue = 4L;
    config.cConfig.map.put("C", "D");
    config.cConfig.enumeration = Enumeration.X;
    config.cConfig.list.addAll(Arrays.asList("E", "F"));
    config.cConfig.dt = new DateTime(10000);

    String json = ConfigUtils.toJson(config);

    TestConfiguration targetConfig = new TestConfiguration();

    // Old values from should be always removed
    targetConfig.aConfig.a2 = "X";
    targetConfig.bConfig.b1 = "Y";
    // Nulls in configs shouldn't be an issue either
    targetConfig.cConfig = null;

    ConfigUtils.fillValues(json, targetConfig);

    assertEquals("A", targetConfig.aConfig.a1);
    assertNull(targetConfig.aConfig.a2);

    assertNull(targetConfig.bConfig.b1);
    assertEquals("B", targetConfig.bConfig.b2);

    assertEquals((Long) 4L, targetConfig.cConfig.longValue);
    assertEquals(1, targetConfig.cConfig.map.size());
    assertTrue(targetConfig.cConfig.map.containsKey("C"));
    assertEquals("D", targetConfig.cConfig.map.get("C"));
    assertEquals(Enumeration.X, targetConfig.cConfig.enumeration);
    assertEquals("E", targetConfig.cConfig.list.get(0));
    assertEquals("F", targetConfig.cConfig.list.get(1));
    assertEquals(10000, targetConfig.cConfig.dt.getMillis());
  }

  /**
   * Config structure that corresponds to Config class declared below
   * @return Config structure
   */
  protected List<MConfig> getConfigs() {
    List<MConfig> ret = new LinkedList<MConfig>();

    List<MInput<?>> inputs;

    // Config A
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MStringInput("aConfig.a1", false, InputEditable.ANY, StringUtils.EMPTY,
        (short) 30, Collections.EMPTY_LIST));
    inputs.add(new MStringInput("aConfig.a2", true, InputEditable.ANY, StringUtils.EMPTY,
        (short) -1, Collections.EMPTY_LIST));
    ret.add(new MConfig("aConfig", inputs, Collections.EMPTY_LIST));

    // Config B
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MStringInput("bConfig.b1", false, InputEditable.ANY, StringUtils.EMPTY,
        (short) 2, Collections.EMPTY_LIST));
    inputs.add(new MStringInput("bConfig.b2", false, InputEditable.ANY, StringUtils.EMPTY,
        (short) 3, Collections.EMPTY_LIST));
    ret.add(new MConfig("bConfig", inputs, Collections.EMPTY_LIST));

    // Config C
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MLongInput("cConfig.longValue", false, InputEditable.ANY, StringUtils.EMPTY, Collections.EMPTY_LIST));
    inputs.add(new MMapInput("cConfig.map", false, InputEditable.ANY, StringUtils.EMPTY, StringUtils.EMPTY, Collections.EMPTY_LIST));
    inputs.add(new MEnumInput("cConfig.enumeration", false, InputEditable.ANY, StringUtils.EMPTY,
        new String[] { "X", "Y" }, Collections.EMPTY_LIST));
    inputs.add(new MListInput("cConfig.list", false, InputEditable.ANY, StringUtils.EMPTY, Collections.EMPTY_LIST));
    inputs.add(new MDateTimeInput("cConfig.dt", false, InputEditable.ANY, StringUtils.EMPTY, Collections.EMPTY_LIST));
    ret.add(new MConfig("cConfig", inputs, Collections.EMPTY_LIST));

    return ret;
  }

  protected List<MConfig> getBadConfigWithSelfOverrideInputs() {
    List<MConfig> ret = new LinkedList<MConfig>();

    List<MInput<?>> inputs;
    // Config A
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MStringInput("aConfig.a1", false, InputEditable.ANY, "aConfig.a1", (short) 30, Collections.EMPTY_LIST));
    inputs.add(new MStringInput("aConfig.a2", true, InputEditable.ANY, StringUtils.EMPTY,
        (short) -1, Collections.EMPTY_LIST));
    ret.add(new MConfig("aConfig", inputs, Collections.EMPTY_LIST));
    return ret;
  }

  protected List<MConfig> getBadConfigWithNonExistingOverrideInputs() {
    List<MConfig> ret = new LinkedList<MConfig>();

    List<MInput<?>> inputs;
    // Config A
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MStringInput("aConfig.a1", false, InputEditable.ANY, "aConfig.a3", (short) 30, Collections.EMPTY_LIST));
    inputs.add(new MStringInput("aConfig.a2", true, InputEditable.ANY, StringUtils.EMPTY,
        (short) -1, Collections.EMPTY_LIST));
    ret.add(new MConfig("aConfig", inputs, Collections.EMPTY_LIST));
    return ret;
  }

  protected List<MConfig> getBadConfigWithUserEditableOverrideInputs() {
    List<MConfig> ret = new LinkedList<MConfig>();

    List<MInput<?>> inputs;
    // Config A
    inputs = new LinkedList<MInput<?>>();
    inputs.add(new MStringInput("aConfig.a1", false, InputEditable.ANY, "aConfig.a2", (short) 30, Collections.EMPTY_LIST));
    inputs.add(new MStringInput("aConfig.a2", true, InputEditable.USER_ONLY, StringUtils.EMPTY,
        (short) -1, Collections.EMPTY_LIST));
    ret.add(new MConfig("aConfig", inputs, Collections.EMPTY_LIST));
    return ret;
  }

  @ConfigurationClass
  public static class ConfigurationWithNonUniqueConfigNameAttribute {
    public ConfigurationWithNonUniqueConfigNameAttribute() {
      aConfig = new InvalidConfig();
      bConfig = new InvalidConfig();
    }

    @Config(name = "sameName")
    InvalidConfig aConfig;
    @Config(name = "sameName")
    InvalidConfig bConfig;
  }

  @ConfigurationClass
  public static class ConfigurationWithInvalidConfigNameAttribute {
    public ConfigurationWithInvalidConfigNameAttribute() {
      invalidConfig = new InvalidConfig();
    }

    @Config(name = "#_config")
    InvalidConfig invalidConfig;
  }

  @ConfigurationClass
  public static class ConfigurationWithInvalidConfigNameAttributeLength {
    public ConfigurationWithInvalidConfigNameAttributeLength() {
      invalidLengthConfig = new InvalidConfig();
    }

    @Config(name = "longest_config_more_than_30_characers")
    InvalidConfig invalidLengthConfig;
  }

  @ConfigurationClass
  public static class TestBadConfiguration {

    public TestBadConfiguration() {
      aBadConfig = new ABadConfig();
    }

    @Config
    ABadConfig aBadConfig;
  }

  @ConfigurationClass
  public static class TestBadConfiguration1 {

    public TestBadConfiguration1() {
      aBadConfig1 = new ABadConfig1();
    }

    @Config
    ABadConfig1 aBadConfig1;
  }

  @ConfigurationClass
  public static class TestBadConfiguration2 {

    public TestBadConfiguration2() {
      aBadConfig2 = new ABadConfig2();
    }

    @Config
    ABadConfig2 aBadConfig2;
  }

  @ConfigurationClass
  public static class TestConfiguration {

    public TestConfiguration() {
      aConfig = new AConfig();
      bConfig = new BConfig();
      cConfig = new CConfig();
    }

    @Config
    AConfig aConfig;
    @Config
    BConfig bConfig;
    @Config
    CConfig cConfig;
  }

  @ConfigurationClass
  public static class PrimitiveConfig {
    @Config
    DConfig dConfig;
  }

  @ConfigClass(validators = {@Validator(AConfig.AConfigValidator.class)})
  public static class AConfig {
    @Input(size = 30, validators = {@Validator(AConfig.A1Validator.class)})
    String a1;
    @Input(sensitive = true)
    String a2;

    public static class AConfigValidator extends AbstractValidator<AConfig> {
      @Override
      public void validate(AConfig aConfig) {
        if (Strings.isNullOrEmpty(aConfig.a1)) {
          addMessage(Status.ERROR, "a1 cannot be empty");
        }
      }
    }

    public static class A1Validator extends AbstractValidator<String> {
      @Override
      public void validate(String a1) {
        if (Strings.isNullOrEmpty(a1)) {
          addMessage(Status.ERROR, "I am a redundant validator");
        }
      }
    }
  }

  @ConfigClass
  public static class ABadConfig {
    @Input(size = 30, editable = InputEditable.USER_ONLY, overrides = "a5")
    String a1;
    @Input(sensitive = true)
    String a2;
  }

  @ConfigClass
  public static class ABadConfig1 {
    @Input(size = 30, editable = InputEditable.USER_ONLY, overrides = "a2")
    String a1;
    @Input(sensitive = true, editable = InputEditable.USER_ONLY, overrides = "a1")
    String a2;
  }

  @ConfigClass
  public static class ABadConfig2 {
    @Input(size = 30, editable = InputEditable.USER_ONLY, overrides = "a1")
    String a1;
    @Input(sensitive = true, editable = InputEditable.USER_ONLY, overrides = "a2")
    String a2;
  }

  @ConfigClass
  public static class BConfig {
    @Input(size = 2)
    String b1;
    @Input(size = 3)
    String b2;
  }

  @ConfigClass
  public static class CConfig {
    @Input
    Long longValue;
    @Input
    Map<String, String> map;
    @Input
    Enumeration enumeration;
    @Input
    List<String> list;
    @Input
    DateTime dt;

    public CConfig() {
      map = new HashMap<String, String>();
      list = new LinkedList<String>();
    }
  }

  @ConfigClass
  public static class InvalidConfig {

  }

  @ConfigClass
  public static class DConfig {
    @Input
    int value;
  }

  public static class ConfigWithoutAnnotation {
  }

  enum Enumeration {
    X, Y,
  }
}
