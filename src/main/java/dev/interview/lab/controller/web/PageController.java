package dev.interview.lab.controller.web;

import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the small server-rendered dashboard pages. */
@Controller
public class PageController {
  private final Environment environment;

  public PageController(Environment environment) {
    this.environment = environment;
  }

  @GetMapping("/")
  public String home(Model model) {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      profiles = environment.getDefaultProfiles();
    }
    model.addAttribute("standalone", Arrays.asList(profiles).contains("standalone"));
    return "index";
  }

  @GetMapping("/h2")
  public String h2() {
    return "h2";
  }

  @GetMapping("/mongo")
  public String mongo() {
    return "mongo";
  }

  @GetMapping("/kafka")
  public String kafka() {
    return "kafka";
  }

  @GetMapping("/rabbitmq")
  public String rabbitmq() {
    return "rabbitmq";
  }
}
