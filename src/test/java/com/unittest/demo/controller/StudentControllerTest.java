package com.unittest.demo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionSystemException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unittest.demo.exception.StudentNotFoundException;
import com.unittest.demo.model.Student;
import com.unittest.demo.repository.StudentRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class StudentControllerTest {
    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentRepository studentRepositoryMock;

    @InjectMocks
    private StudentController studentController;

    @Before
    public void init () {
    }


    @Test
    public void findAll_StudentsFound_ShouldReturnFoundStudentEntries () throws Exception {
        Student first  = new Student(1l, "Bob", "A1234567");
        Student second = new Student(2l, "Alice", "B1234568");

        when(studentRepositoryMock.findAll()).thenReturn(Arrays.asList(first, second));

        mockMvc.perform(get("/student"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id", is(1)))
               .andExpect(jsonPath("$[0].name", is("Bob")))
               .andExpect(jsonPath("$[0].passportNumber", is("A1234567")))
               .andExpect(jsonPath("$[1].id", is(2)))
               .andExpect(jsonPath("$[1].name", is("Alice")))
               .andExpect(jsonPath("$[1].passportNumber", is("B1234568")));

        verify(studentRepositoryMock, times(1)).findAll();
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void findAll_EmptyResultData_ShouldReturnEntries () throws Exception {

        when(studentRepositoryMock.findAll()).thenThrow(EmptyResultDataAccessException.class);

        mockMvc.perform(get("/student"))
               .andExpect(status().isNotFound());

        verify(studentRepositoryMock, times(1)).findAll();
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void findById_StudentNotFound_ShouldThrowStudentNotFoundException () throws Exception {
        Optional<Student> student = Optional.of(new Student(1l, "Bob", "A1234567"));

        when(studentRepositoryMock.findById(1l)).thenReturn(Optional.empty());;

        mockMvc.perform(get("/student/" + 1l))
               .andExpect(status().isNotFound());

        verify(studentRepositoryMock, times(1)).findById(1l);
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void findById_StudentFound_ShouldReturnFoundStudentEntry () throws Exception {
        Optional<Student> student = Optional.of(new Student(1l, "Bob", "A1234567"));

        when(studentRepositoryMock.findById(1l)).thenReturn(student);

        mockMvc.perform(get("/student/" + 1l))
               .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name", is("Bob")))
               .andExpect(jsonPath("$.passportNumber", is("A1234567")));

        verify(studentRepositoryMock, times(1)).findById(1l);
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void addStudent_ValidationFails_ShouldReturnStatusCode400 () throws Exception {
        Student student = new Student(1l,
                                      "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija",
                                      "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija");
        String content = "{\n    \"name\": \"abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija\"," +
                         "\n    \"passportNumber\": \"abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija\"\n}";
        when(studentRepositoryMock.save(anyObject())).thenReturn(student);

        mockMvc.perform(post("/student")
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().isBadRequest());

        verify(studentRepositoryMock, times(0)).save(anyObject());
    }

    @Test
    public void addStudent_TransactionFail_ShouldReturnStatusCode400 () throws Exception {
        Student student = new Student("Bob", "A1234567");

        when(studentRepositoryMock.save(anyObject())).thenThrow(TransactionSystemException.class);

        mockMvc.perform(post("/student")
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().isBadRequest());

        verify(studentRepositoryMock, times(1)).save(anyObject());
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void addStudent_ValidationPass_ShouldReturnStatusCode201 () throws Exception {
        Student student = new Student("Bob", "A1234567");

        when(studentRepositoryMock.save(anyObject())).thenReturn(student);

        mockMvc.perform(post("/student")
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().is2xxSuccessful());

        verify(studentRepositoryMock, times(1)).save(anyObject());
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void deleteStudent_StudentNotFound_ShouldThrowStudentNotFoundException () throws Exception {
        doThrow(StudentNotFoundException.class).when(studentRepositoryMock).deleteById(1l);

        mockMvc.perform(delete("/student/" + 1l))
               .andExpect(status().isNotFound());

        verify(studentRepositoryMock, times(1)).deleteById(1l);
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void deleteStudent_StudentFound_ShouldReturnStatusCode200 () throws Exception {
        doNothing().when(studentRepositoryMock).deleteById(1l);

        mockMvc.perform(delete("/student/" + 1l))
               .andExpect(status().isOk());

        verify(studentRepositoryMock, times(1)).deleteById(1l);
        verifyNoMoreInteractions(studentRepositoryMock);
    }

    @Test
    public void updateStudent_StudentNotFound_ShouldThrowStudentNotFoundException () throws Exception {
        Student student = new Student(1l, "Bob", "A1234567");
        when(studentRepositoryMock.findById(1l)).thenReturn(Optional.empty());;
        when(studentRepositoryMock.save(anyObject())).thenReturn(student);

        mockMvc.perform(put("/student/" + 1l)
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().isNotFound());

        verify(studentRepositoryMock, times(1)).findById(1l);
        verify(studentRepositoryMock, times(0)).save(anyObject());
    }

    @Test
    public void updateStudent_ValidationFails_ShouldReturnStatusCode400 () throws Exception {
        Student student = new Student(1l,
                                      "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija",
                                      "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghija");

        when(studentRepositoryMock.findById(1l)).thenReturn(Optional.ofNullable(student));
        when(studentRepositoryMock.save(anyObject())).thenReturn(student);

        mockMvc.perform(put("/student/" + 1l)
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().isBadRequest());

        verify(studentRepositoryMock, times(0)).save(anyObject());
    }

    @Test
    public void updateStudent_ValidationPass_ShouldReturnStatusCode201 () throws Exception {
        Student student = new Student(1l, "Bob", "A1234567");

        when(studentRepositoryMock.findById(1l)).thenReturn(Optional.ofNullable(student));
        when(studentRepositoryMock.save(anyObject())).thenReturn(student);

        mockMvc.perform(put("/student/" + 1l)
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(asJsonString(student)))
               .andExpect(status().is2xxSuccessful());

        verify(studentRepositoryMock, times(1)).findById(1l);
        verify(studentRepositoryMock, times(1)).save(anyObject());
    }

    public static String asJsonString (final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

