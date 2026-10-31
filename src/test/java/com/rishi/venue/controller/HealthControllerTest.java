package com.rishi.venue.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        HealthController controller = new HealthController(dataSource);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Health check returns 200 OK and UP status when database is healthy")
    void shouldReturnHealthyWhenDatabaseConnected() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(true);

        mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("venue-booking"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.database.details").value("connected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Health check returns 503 Service Unavailable and DOWN status when database fails")
    void shouldReturnDownWhenDatabaseFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection refused"));

        mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.database.status").value("DOWN"))
                .andExpect(jsonPath("$.components.database.details").isNotEmpty());
    }

    @Test
    @DisplayName("Health check returns 200 OK when dataSource is null")
    void shouldReturnUpWhenDataSourceIsNull() throws Exception {
        MockMvc nullDsMockMvc = MockMvcBuilders.standaloneSetup(new HealthController(null)).build();
        nullDsMockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
