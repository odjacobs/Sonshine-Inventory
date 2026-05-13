package net.disc0.sonshine_inventory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAdvice {

    private final String appVersion;

    @Autowired
    public GlobalModelAdvice(Optional<BuildProperties> buildProperties) {
        this.appVersion = buildProperties.map(BuildProperties::getVersion).orElse("dev");
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersion;
    }
}
