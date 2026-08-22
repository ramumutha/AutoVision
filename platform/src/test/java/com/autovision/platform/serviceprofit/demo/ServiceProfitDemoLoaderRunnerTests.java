package com.autovision.platform.serviceprofit.demo;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class ServiceProfitDemoLoaderRunnerTests {

    @Test
    void runnerIsRestrictedToServiceProfitDemoProfile() {

        Profile profile =
                ServiceProfitDemoLoaderRunner.class
                        .getAnnotation(Profile.class);

        assertNotNull(
                profile,
                "Demo loader runner must be profile-gated"
        );

        String[] values = profile.value();

        Arrays.sort(values);

        assertArrayEquals(
                new String[] {
                        "service-profit-demo"
                },
                values
        );
    }

    @Test
    void disabledRunnerDoesNotInvokeLoader()
            throws Exception {

        ServiceProfitDemoLoader loader =
                mock(ServiceProfitDemoLoader.class);

        ServiceProfitDemoMaterializer materializer =
                mock(ServiceProfitDemoMaterializer.class);

        ServiceProfitDemoLoaderRunner runner =
                new ServiceProfitDemoLoaderRunner(
                        loader,
                        materializer
                );

        ReflectionTestUtils.setField(
                runner,
                "enabled",
                false
        );

        ReflectionTestUtils.setField(
                runner,
                "datasetRoot",
                "../demo-data/service-profit/r1"
        );

        ReflectionTestUtils.setField(
                runner,
                "principalId",
                UUID.fromString(
                        "00000000-0000-4000-8000-000000000001"
                )
        );

        runner.run(
                mock(
                        org.springframework.boot.ApplicationArguments.class
                )
        );

        verify(
                loader,
                never()
        ).load(
                any(Path.class),
                any(UUID.class),
                any()
        );

        verify(
                materializer,
                never()
        ).materialize(
                any(Path.class),
                any(UUID.class),
                any()
        );
    }

    @Test
    void enabledRunnerInvokesLoaderWithConfiguredDataset()
            throws Exception {

        ServiceProfitDemoLoader loader =
                mock(ServiceProfitDemoLoader.class);

        ServiceProfitDemoMaterializer materializer =
                mock(ServiceProfitDemoMaterializer.class);

        ServiceProfitDemoLoaderRunner runner =
                new ServiceProfitDemoLoaderRunner(
                        loader,
                        materializer
                );

        UUID principalId =
                UUID.fromString(
                        "00000000-0000-4000-8000-000000000001"
                );

        String datasetRoot =
                "../demo-data/service-profit/r1";

        ReflectionTestUtils.setField(
                runner,
                "enabled",
                true
        );

        ReflectionTestUtils.setField(
                runner,
                "datasetRoot",
                datasetRoot
        );

        ReflectionTestUtils.setField(
                runner,
                "principalId",
                principalId
        );

        ServiceProfitDemoLoadResult result =
                new ServiceProfitDemoLoadResult(
                        true,
                        false,
                        UUID.randomUUID(),
                        "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                        "1.0.0"
                );

        org.mockito.Mockito.when(
                loader.load(
                        any(Path.class),
                        eq(principalId),
                        any()
                )
        ).thenReturn(result);

        org.mockito.Mockito.when(
                materializer.materialize(
                        any(Path.class),
                        eq(principalId),
                        any()
                )
        ).thenReturn(
                ServiceProfitDemoMaterializationResult.from(
                        List.of()
                )
        );

        runner.run(
                mock(
                        org.springframework.boot.ApplicationArguments.class
                )
        );

        ArgumentCaptor<OffsetDateTime> loadTime =
                ArgumentCaptor.forClass(OffsetDateTime.class);

        ArgumentCaptor<OffsetDateTime> materializationTime =
                ArgumentCaptor.forClass(OffsetDateTime.class);

        Path expectedRoot =
                Path.of(datasetRoot)
                        .toAbsolutePath()
                        .normalize();

        verify(loader).load(
                eq(
                        expectedRoot
                ),
                eq(principalId),
                loadTime.capture()
        );

        verify(materializer).materialize(
                eq(expectedRoot),
                eq(principalId),
                materializationTime.capture()
        );

        assertEquals(
                loadTime.getValue(),
                materializationTime.getValue()
        );
    }
}
