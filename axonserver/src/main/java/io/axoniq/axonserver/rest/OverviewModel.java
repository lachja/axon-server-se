/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.rest;

import io.axoniq.axonserver.logging.AuditLog;
import io.axoniq.axonserver.rest.svg.Element;
import io.axoniq.axonserver.rest.svg.Elements;
import io.axoniq.axonserver.rest.svg.Fonts;
import io.axoniq.axonserver.rest.svg.attribute.Position;
import io.axoniq.axonserver.rest.svg.attribute.StyleClass;
import io.axoniq.axonserver.rest.svg.decorator.Clickable;
import io.axoniq.axonserver.rest.svg.decorator.Grouped;
import io.axoniq.axonserver.rest.svg.element.Rectangle;
import io.axoniq.axonserver.rest.svg.mapping.Application;
import io.axoniq.axonserver.rest.svg.mapping.ApplicationBoxMapping;
import io.axoniq.axonserver.rest.svg.mapping.AxonServer;
import io.axoniq.axonserver.rest.svg.mapping.AxonServerBoxMapping;
import io.axoniq.axonserver.rest.svg.mapping.AxonServerPopupMapping;
import io.axoniq.axonserver.topology.Topology;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

/**
 * Controller that generates a SVG model of the configuration of AxonServer and the connected client applications.
 *
 * @author Marc Gathier
 */
@RestController("OverviewModel")
public class OverviewModel {

    private static final Logger auditLog = AuditLog.getLogger();

    private final Topology clusterController;
    private final Iterable<Application> applicationProvider;
    private final Iterable<AxonServer> axonServerProvider;
    private final AxonServersOverviewProvider axonServersOverviewProvider;
    private final Fonts fonts;

    public OverviewModel(Topology clusterController,
                         Iterable<Application> applicationProvider,
                         Iterable<AxonServer> axonServerProvider, AxonServersOverviewProvider axonServersOverviewProvider) {
        this.clusterController = clusterController;
        this.applicationProvider = applicationProvider;
        this.axonServerProvider = axonServerProvider;
        this.axonServersOverviewProvider = axonServersOverviewProvider;
        this.fonts = new Fonts();
    }

    @GetMapping("/v1/public/overview")
    public SvgOverview overview(final Principal principal) {
        auditLog.debug("[{}] Request to render an SVG cluster overview.", AuditLog.username(principal));

        boolean multiContext = clusterController.isMultiContext();
        AxonServerBoxMapping serverRegistry = new AxonServerBoxMapping(multiContext, clusterController.getName(), fonts);

        Element hubNodes = new Grouped(new Elements(10, 200, axonServerProvider, serverRegistry), "axonserverNodes");
        Element clients = new Elements(10, 10, applicationProvider, new ApplicationBoxMapping(serverRegistry, fonts));
        Element popups = new Elements(axonServerProvider, new AxonServerPopupMapping(serverRegistry, fonts));

        Elements components = new Elements(hubNodes, clients, popups);
        Element background = new Clickable(new Rectangle(new Position(0, 0),
                                            components.dimension().increase(20, 20),
                                            new StyleClass("background")), ()-> "hidePopup()");

        return new SvgOverview(new Elements(background, components));
    }

    public static class SvgOverview {
        private final Element element;

        SvgOverview(Element element) {
            this.element = element;
        }

        public String getSvgObjects() {
            StringWriter stringWriter = new StringWriter();
            element.printOn(new PrintWriter(stringWriter));
            return stringWriter.toString();
        }
        public int getWidth() {
            return element.dimension().width();
        }
        public int getHeight() {
            return element.dimension().height();
        }
    }

}
