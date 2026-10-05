package dev.interview.lab.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.interview.lab.messaging.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:order_controller_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.kafka.listener.auto-startup=false",
      "spring.rabbitmq.listener.simple.auto-startup=false"
    })
@AutoConfigureMockMvc
class OrderControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean OrderEventPublisher publisher;

  @Test
  void listsLiquibaseSeedData() throws Exception {
    mvc.perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerName").value("Ada Lovelace"));
  }

  @Test
  void servesDashboardAndH2PageWithoutExternalServices() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Standalone mode is active")));
    mvc.perform(get("/h2")).andExpect(status().isOk());
  }

  @Test
  void validatesAndCreatesOrder() throws Exception {
    mvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Linus Torvalds\",\"total\":19.99}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CREATED"));
    mvc.perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerName == 'Linus Torvalds')]").isNotEmpty());
    mvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\" \",\"total\":0}"))
        .andExpect(status().isBadRequest());
  }
}
