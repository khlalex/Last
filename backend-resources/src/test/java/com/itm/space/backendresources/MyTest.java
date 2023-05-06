package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test", password = "test", authorities = "ROLE_MODERATOR")
public class MyTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realmItm;
    @Mock
    private RoleMappingResource roleMappingResource;
    @Mock
    private MappingsRepresentation mappingsRepresentation;

    private UserRequest testUserRequest;
    private UserRequest testInvalidUserRequest;
    private RealmResource realmResourceMock;
    private UsersResource usersResourceMock;
    private UserRepresentation userRepresentationMock;
    private UserResource userResourceMock;
    private UUID testId;

    @BeforeEach
    void initNecessaryMocks() {
        testUserRequest = new UserRequest(
                "alex",
                "alex@gmail.com",
                "qwerty",
                "alex",
                "alexov");
        testInvalidUserRequest = new UserRequest(
                "alex",
                "@gmail.com",
                "",
                "Aleksandr",
                "");
        realmResourceMock = mock(RealmResource.class);
        usersResourceMock = mock(UsersResource.class);
        userRepresentationMock = mock(UserRepresentation.class);
        userResourceMock = mock(UserResource.class);
        testId = UUID.randomUUID();
    }


    @Test
    @SneakyThrows
    public void helloControllerTest() {
        MockHttpServletResponse mockHttpServletResponse = mvc.perform(get("/api/users/hello"))
                .andReturn()
                .getResponse();
        assertEquals(HttpStatus.OK.value(), mockHttpServletResponse.getStatus());
        assertEquals("test", mockHttpServletResponse.getContentAsString());
    }

    @Test
    @SneakyThrows
    public void userCreatedTest() {
        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(usersResourceMock.create(any())).thenReturn(Response.status(Response.Status.CREATED).build());
        when(userRepresentationMock.getId()).thenReturn(UUID.randomUUID().toString());
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testUserRequest))
                .andReturn().getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(keycloak).realm(realmItm);
        verify(realmResourceMock).users();
        verify(usersResourceMock).create(any(UserRepresentation.class));
    }
    @Test
    @SneakyThrows
    public void UserCreatedTestFail(){
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testInvalidUserRequest))
                .andDo(print())
                .andReturn().getResponse();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }
    @Test
    @SneakyThrows
    public void getUserByIdTest() {

        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(usersResourceMock.get(String.valueOf(testId))).thenReturn(userResourceMock);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(String.valueOf(testId));
        userRepresentation.setFirstName("alex");
        userRepresentation.setLastName("alex");
        userRepresentation.setEmail("alex@mail.ru");

        when(userResourceMock.toRepresentation()).thenReturn(userRepresentation);
        when(userResourceMock.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);

        MockHttpServletResponse response = mvc.perform(get("/api/users/" + testId))
                .andDo(print())
                .andExpect(jsonPath("$.firstName").value("alex"))
                .andReturn()
                .getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @SneakyThrows
    public void getUserByIdTestFail() {
        UUID userId = UUID.randomUUID();
        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(mock(UsersResource.class));
        when(realmResourceMock.users().get(eq(String.valueOf(userId)))).thenReturn(null);

        MockHttpServletResponse response = mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isInternalServerError())
                .andDo(print())
                .andReturn().getResponse();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
    }
}