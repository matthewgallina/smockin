package com.smockin.admin.service.utils;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.class)
public class RestfulMockServiceUtilsTest {

    @Mock
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Mock
    private RestfulMockSortingUtils restfulMockSortingUtils;

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

}
