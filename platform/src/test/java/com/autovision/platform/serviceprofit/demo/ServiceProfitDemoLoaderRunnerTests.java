package com.autovision.platform.serviceprofit.demo;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

        ServiceProfitDemoLoaderRunner runner =
                new ServiceProfitDemoLoaderRunner(
                        loader
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
    }

    @Test
    void enabledRunnerInvokesLoaderWithConfiguredDataset()
            throws Exception {

        ServiceProfitDemoLoader loader =
                mock(ServiceProfitDemoLoader.class);

        ServiceProfitDemoLoaderRunner runner =
                new ServiceProfitDemoLoaderRunner(
                        loader
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

        runner.run(
                mock(
                        org.springframework.boot.ApplicationArguments.class
                )
        );

        verify(loader).load(
                eq(
                        Path.of(datasetRoot)
                                .toAbsolutePath()
                                .normalize()
                ),
                eq(principalId),
                any()
        );
    }
}
