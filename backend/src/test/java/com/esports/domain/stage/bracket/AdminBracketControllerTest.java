package com.esports.domain.stage.bracket;

import com.esports.config.JwtTokenProvider;
import com.esports.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBracketController.class)
@Import(SecurityConfig.class)  // 실제 필터 체인으로 401 검증 (Hard Rule #7)
class AdminBracketControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean BracketSyncService syncService;
    @MockBean BracketQueryService queryService;
    @MockBean BracketAdminService adminService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void syncWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/stages/1/bracket/sync"))
                .andExpect(status().isUnauthorized());
        verify(syncService, never()).sync(anyLong());
    }

    @Test
    void resetWithoutAuthReturns401() throws Exception {
        mockMvc.perform(delete("/api/admin/stages/1/bracket"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void slotUpdateWithoutAuthReturns401() throws Exception {
        mockMvc.perform(put("/api/admin/bracket-nodes/1/slots/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"LABEL\",\"label\":\"레전드 1위\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void slotUpdateRejectsTooLongLabel() throws Exception {
        String label = "가".repeat(101);
        mockMvc.perform(put("/api/admin/bracket-nodes/1/slots/0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"LABEL\",\"label\":\"" + label + "\"}"))
                .andExpect(status().isBadRequest());
        verify(adminService, never()).updateSlot(anyLong(), anyInt(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void layoutUpdateRejectsOutOfRange() throws Exception {
        mockMvc.perform(patch("/api/admin/bracket-nodes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnOverride\":16}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncReturns200ForAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/stages/1/bracket/sync").with(csrf()))
                .andExpect(status().isOk());
    }
}
