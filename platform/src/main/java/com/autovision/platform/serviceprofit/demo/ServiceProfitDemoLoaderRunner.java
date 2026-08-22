package com.autovision.platform.serviceprofit.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Profile("service-profit-demo")
public class ServiceProfitDemoLoaderRunner
        implements ApplicationRunner {

    private final ServiceProfitDemoLoader loader;
        private final ServiceProfitDemoMaterializer materializer;

    @Value(
            "${autovision.service-profit.demo-load.enabled:false}"
    )
    private boolean enabled;

    @Value(
            "${autovision.service-profit.demo-load.dataset-root:../demo-data/service-profit/r1}"
    )
    private String datasetRoot;

    @Value(
            "${autovision.service-profit.demo-load.principal-id:00000000-0000-4000-8000-000000000001}"
    )
    private UUID principalId;

    public ServiceProfitDemoLoaderRunner(
                        ServiceProfitDemoLoader loader,
                        ServiceProfitDemoMaterializer materializer
    ) {
        this.loader = loader;
                this.materializer = materializer;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {
        if (!enabled) {
            return;
        }

        Path root =
                Path.of(datasetRoot)
                        .toAbsolutePath()
                        .normalize();

        OffsetDateTime startupTime =
                OffsetDateTime.now();

        ServiceProfitDemoLoadResult result =
                loader.load(
                        root,
                        principalId,
                        startupTime
                );

        ServiceProfitDemoMaterializationResult materializationResult =
                materializer.materialize(
                        root,
                        principalId,
                        startupTime
                );

        if (result.loaded()) {
            System.out.println(
                    "PASS: Service Profit demo dataset loaded "
                            + result.datasetId()
                            + " "
                            + result.datasetVersion()
            );
        } else if (result.alreadyPresent()) {
            System.out.println(
                    "PASS: Service Profit demo dataset already loaded "
                            + result.datasetId()
                            + " "
                            + result.datasetVersion()
            );
        }

                System.out.println(
                                "PASS: Service Profit demo opportunities materialized "
                                                + materializationResult.totalScenariosEvaluated()
                                                + " scenarios"
                );
    }
}
