package com.smockin.admin.service.utils;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.service.SmockinUserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.class)
public class RestfulMockServiceUtilsTest {

    @Mock
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Mock
    private RestfulMockSortingUtils restfulMockSortingUtils;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private SmockinUserDAO smockinUserDAO;


    @Spy
    @InjectMocks
    private RestfulMockServiceUtils utils;

    private RestfulMockDTO dto;


    @Before
    public void setUp() {
        dto = new RestfulMockDTO();
    }

    @Test
    public void amendPath_PrefixAdded_Test() {

        // Setup
        dto.setPath("foo");

        // Test
        utils.amendPath(dto);

        // Assertions
        Assert.assertEquals("/foo", dto.getPath());

    }

    @Test
    public void amendPath_NothingToChange_Test() {

        // Setup
        dto.setPath("/foo");

        // Test
        utils.amendPath(dto);

        // Assertions
        Assert.assertEquals("/foo", dto.getPath());

    }

    @Test
    public void validateMockPathDoesNotStartWithUsername_1partPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("/bob");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assert.assertNotNull(pathSegment);
        Assert.assertEquals("bob", pathSegment);

    }

    @Test
    public void validateMockPathDoesNotStartWithUsername_1partNonPrefixedPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("bob");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assert.assertNotNull(pathSegment);
        Assert.assertEquals("bob", pathSegment);

    }

    @Test
    public void validateMockPathDoesNotStartWithUsername_2partPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("/bob/house");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assert.assertNotNull(pathSegment);
        Assert.assertEquals("bob", pathSegment);

    }

    @Test
    public void validateMockPathDoesNotStartWithUsername_2partNonPrefixedPath_Test() throws ValidationException {

        // Setup
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);
        Mockito.when(smockinUserDAO.existsSmockinUserByUsername(Mockito.anyString())).thenReturn(false);

        // Test
        utils.validateMockPathDoesNotStartWithUsername("bob/house");

        // Assertions
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(smockinUserDAO, Mockito.times(1)).existsSmockinUserByUsername(captor.capture());
        final String pathSegment = captor.getValue();
        Assert.assertNotNull(pathSegment);
        Assert.assertEquals("bob", pathSegment);

    }

}
