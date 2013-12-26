/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.distribution.repository.mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openlmis.core.domain.*;
import org.openlmis.core.query.QueryExecutor;
import org.openlmis.core.repository.mapper.*;
import org.openlmis.db.categories.IntegrationTests;
import org.openlmis.distribution.builder.DistributionBuilder;
import org.openlmis.distribution.domain.Distribution;
import org.openlmis.distribution.domain.DistributionRefrigerators;
import org.openlmis.distribution.domain.RefrigeratorProblem;
import org.openlmis.distribution.domain.RefrigeratorReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openlmis.core.builder.DeliveryZoneBuilder.defaultDeliveryZone;
import static org.openlmis.core.builder.FacilityBuilder.defaultFacility;
import static org.openlmis.core.builder.ProcessingPeriodBuilder.defaultProcessingPeriod;
import static org.openlmis.core.builder.ProcessingPeriodBuilder.scheduleId;
import static org.openlmis.core.builder.ProcessingScheduleBuilder.defaultProcessingSchedule;
import static org.openlmis.core.builder.ProgramBuilder.defaultProgram;
import static org.openlmis.distribution.builder.DistributionBuilder.*;

@Category(IntegrationTests.class)
@ContextConfiguration(locations = "classpath:test-applicationContext-distribution.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true, transactionManager = "openLmisTransactionManager")
public class DistributionRefrigeratorsMapperIT {

  @Autowired
  private DistributionRefrigeratorsMapper mapper;

  @Autowired
  private FacilityMapper facilityMapper;

  @Autowired
  DeliveryZoneMapper deliveryZoneMapper;

  @Autowired
  ProgramMapper programMapper;

  @Autowired
  ProcessingPeriodMapper periodMapper;

  @Autowired
  DistributionMapper distributionMapper;

  @Autowired
  private ProcessingScheduleMapper scheduleMapper;

  @Autowired
  private RefrigeratorMapper refrigeratorMapper;

  @Autowired
  private QueryExecutor queryExecutor;

  DeliveryZone zone;
  Program program;
  ProcessingPeriod processingPeriod;
  Facility facility;
  Distribution distribution;

  @Before
  public void setUp() throws Exception {
    zone = make(a(defaultDeliveryZone));
    program = make(a(defaultProgram));

    ProcessingSchedule schedule = make(a(defaultProcessingSchedule));
    scheduleMapper.insert(schedule);

    processingPeriod = make(a(defaultProcessingPeriod, with(scheduleId, schedule.getId())));

    deliveryZoneMapper.insert(zone);
    programMapper.insert(program);
    periodMapper.insert(processingPeriod);

    facility = make(a(defaultFacility));
    facilityMapper.insert(facility);

    distribution = make(a(initiatedDistribution,
      with(deliveryZone, zone),
      with(period, processingPeriod),
      with(DistributionBuilder.program, program)));
    distributionMapper.insert(distribution);
  }

  @Test
  public void shouldInsertDistributionRefrigerators() throws SQLException {
    RefrigeratorReading reading = new RefrigeratorReading();
    DistributionRefrigerators distributionRefrigerators = new DistributionRefrigerators(facility, distribution, asList(reading));

    mapper.insert(distributionRefrigerators);

    ResultSet resultSet = queryExecutor.execute("SELECT * FROM distribution_refrigerators WHERE id = " + distributionRefrigerators.getId());
    assertTrue(resultSet.next());
    assertThat(resultSet.getLong("facilityId"), is(facility.getId()));
  }

  @Test
  public void shouldInsertReadingForADistributionRefrigerator() throws SQLException {
    Refrigerator refrigerator = new Refrigerator("SAM", "SAM", "LG", facility.getId());
    refrigerator.setCreatedBy(1L);
    refrigerator.setModifiedBy(1L);
    RefrigeratorReading reading = new RefrigeratorReading(refrigerator);
    reading.setTemperature(98.6F);
    reading.setFunctioningCorrectly("Y");
    DistributionRefrigerators distributionRefrigerators = new DistributionRefrigerators(facility, distribution, asList(reading));

    refrigeratorMapper.insert(refrigerator);
    mapper.insert(distributionRefrigerators);

    reading.setDistributionRefrigeratorsId(distributionRefrigerators.getId());
    mapper.insertReading(reading);

    ResultSet resultSet = queryExecutor.execute("SELECT * FROM refrigerator_readings WHERE distributionRefrigeratorsId = " + distributionRefrigerators.getId());
    assertTrue(resultSet.next());
    assertThat(resultSet.getFloat("temperature"), is(reading.getTemperature()));
    assertThat(resultSet.getString("refrigeratorSerialNumber"), is(reading.getRefrigerator().getSerialNumber()));
    assertThat(resultSet.getString("refrigeratorBrand"), is(reading.getRefrigerator().getBrand()));
    assertThat(resultSet.getString("refrigeratorModel"), is(reading.getRefrigerator().getModel()));
  }

  @Test
  public void shouldInsertRefrigeratorProblems() throws Exception {
    Refrigerator refrigerator = new Refrigerator("SAM", "SAM", "LG", facility.getId());
    refrigerator.setCreatedBy(1L);
    refrigerator.setModifiedBy(1L);
    RefrigeratorReading reading = new RefrigeratorReading(refrigerator);
    reading.setTemperature(98.6F);
    reading.setFunctioningCorrectly("Y");
    DistributionRefrigerators distributionRefrigerators = new DistributionRefrigerators(facility, distribution, asList(reading));

    refrigeratorMapper.insert(refrigerator);
    mapper.insert(distributionRefrigerators);

    reading.setDistributionRefrigeratorsId(distributionRefrigerators.getId());
    mapper.insertReading(reading);

    RefrigeratorProblem problem = new RefrigeratorProblem(reading.getId(), true, false, true, false, true, false, "No Problem");
    mapper.insertProblems(problem);

    ResultSet resultSet = queryExecutor.execute("SELECT * FROM refrigerator_problems WHERE readingId = " + reading.getId());
    assertTrue(resultSet.next());
    assertThat(resultSet.getBoolean("gasLeakage"), is(problem.getGasLeakage()));
  }

  @Test
  public void shouldGetDistributionRefrigeratorsByFacilityIdAndDistributionId() throws Exception {
    RefrigeratorReading reading = new RefrigeratorReading();
    DistributionRefrigerators distributionRefrigerators = new DistributionRefrigerators(facility, distribution, asList(reading));

    mapper.insert(distributionRefrigerators);

    DistributionRefrigerators savedDistributionRefrigerators = mapper.getBy(distributionRefrigerators.getFacilityId(), distributionRefrigerators.getDistributionId());

    assertThat(savedDistributionRefrigerators.getFacilityId(), is(distributionRefrigerators.getFacilityId()));
    assertThat(savedDistributionRefrigerators.getDistributionId(), is(distributionRefrigerators.getDistributionId()));
  }
}