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
package org.apache.sqoop.driver;

import org.apache.sqoop.driver.configuration.JobConfiguration;
import org.apache.sqoop.driver.configuration.ThrottlingConfig;
import org.apache.sqoop.validation.Status;
import org.apache.sqoop.validation.ConfigValidator;
import org.apache.sqoop.validation.Validator;

public class DriverConfigValidator extends Validator {
  @Override
  public ConfigValidator validateConfigForJob(Object jobConfiguration) {
    ConfigValidator validation = new ConfigValidator(JobConfiguration.class);
    JobConfiguration conf = (JobConfiguration)jobConfiguration;
    validateThrottlingConfig(validation,conf.throttlingConfig);

    return validation;
  };

  private void validateThrottlingConfig(ConfigValidator validation, ThrottlingConfig throttlingConfig) {
    if(throttlingConfig.numExtractors != null && throttlingConfig.numExtractors < 1) {
      validation.addMessage(Status.UNACCEPTABLE, "throttlingConfig", "numExtractors", "You need to specify more than one extractor");
    }

    if(throttlingConfig.numLoaders != null && throttlingConfig.numLoaders < 1) {
      validation.addMessage(Status.UNACCEPTABLE, "throttlingConfig", "numLoaders", "You need to specify more than one loader");
    }
  }

}
