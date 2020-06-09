package edu.iis.mto.testreactor.dishwasher;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishWasherTest {
    @Mock
    private WaterPump waterPump;
    @Mock
    private Engine engine;
    @Mock
    private DirtFilter dirtFilter;
    @Mock
    private Door door;

    private DishWasher dishWasher;
    private ProgramConfiguration programConfiguration;
    private WashingProgram washingProgram;
    private FillLevel fillLevel;

    @BeforeEach
    public void setUp() {
        dishWasher = new DishWasher(waterPump, engine, dirtFilter, door);
    }

    @Test
    public void itCompiles() {
        assertThat(true, Matchers.equalTo(true));
    }

    @Test
    public void shouldRunProgramWithSuccess() {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(100d);
        fillLevel = FillLevel.FULL;
        washingProgram = WashingProgram.ECO;
        programConfiguration = getProgramConfiguration(fillLevel, washingProgram);
        RunResult runResult = dishWasher.start(programConfiguration);

        assertEquals(runResult.getStatus(), Status.SUCCESS);
        assertEquals(runResult.getRunMinutes(), WashingProgram.ECO.getTimeInMinutes());
    }

    @Test
    public void shouldRunProgramWithErrorFilter() {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(50d);
        fillLevel = FillLevel.FULL;
        washingProgram = WashingProgram.ECO;
        programConfiguration = getProgramConfiguration(fillLevel, washingProgram);
        RunResult runResult = dishWasher.start(programConfiguration);

        assertEquals(runResult.getStatus(), Status.ERROR_FILTER);
    }

    @Test
    public void shouldRunProgramWithErrorDoorOpen() {
        when(door.closed()).thenReturn(false);
        fillLevel = FillLevel.FULL;
        washingProgram = WashingProgram.ECO;
        programConfiguration = getProgramConfiguration(fillLevel, washingProgram);
        RunResult runResult = dishWasher.start(programConfiguration);

        assertEquals(runResult.getStatus(), Status.DOOR_OPEN);
    }

    @Test
    public void shouldVerifyCorrectOrderWhenWashingProgramIsRinse() throws PumpException, EngineException {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(100d);
        fillLevel = FillLevel.FULL;
        washingProgram = WashingProgram.RINSE;
        programConfiguration = getProgramConfiguration(fillLevel, washingProgram);
        dishWasher.start(programConfiguration);

        InOrder callOrder = inOrder(waterPump, engine);
        callOrder.verify(waterPump).pour(fillLevel);
        callOrder.verify(engine).runProgram(washingProgram);
        callOrder.verify(waterPump).drain();
    }

    @Test
    public void shouldVerifyCorrectOrderWhenWashingProgramIsNotRinse() throws PumpException, EngineException {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(100d);
        fillLevel = FillLevel.FULL;
        washingProgram = WashingProgram.NIGHT;
        programConfiguration = getProgramConfiguration(fillLevel, washingProgram);
        dishWasher.start(programConfiguration);

        InOrder callOrder = inOrder(waterPump, engine);
        callOrder.verify(waterPump).pour(fillLevel);
        callOrder.verify(engine).runProgram(washingProgram);
        callOrder.verify(waterPump).drain();
        callOrder.verify(waterPump).pour(FillLevel.FULL);
        callOrder.verify(engine).runProgram(WashingProgram.RINSE);
        callOrder.verify(waterPump).drain();
    }

    private ProgramConfiguration getProgramConfiguration(FillLevel fillLevel, WashingProgram washingProgram) {
        return ProgramConfiguration.builder()
                .withTabletsUsed(true)
                .withFillLevel(fillLevel)
                .withProgram(washingProgram)
                .build();
    }
}
