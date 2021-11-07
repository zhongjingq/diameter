package com.shrewdify.diaroser.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ NegativeTestCase.class, DataBaseConnect.class,ElementParserTEst.class,PeerImplSessionTest.class,PeerImplTest.class,ServerIoTest.class})
public class TestSuiteAll{}
