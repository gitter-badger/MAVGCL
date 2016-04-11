package com.comino.flight.control.integration;

import com.comino.mq.bus.IMWMessageBusListener;
import com.comino.mq.bus.MWMessageBus;
import com.comino.mq.tests.OptPos;
import com.comino.msp.model.DataModel;

public class SlamTest {

	private OptPos pos = null;
	private long tms=0;
	private DataModel model = null;

	public SlamTest(DataModel model) {

		this.model = model;

		MWMessageBus bus1 = new MWMessageBus(1,"127.0.0.1");

		bus1.addListener(new IMWMessageBusListener() {

			@Override
			public void received(Object o) {

				pos = (OptPos)o;
				System.out.println(System.currentTimeMillis()-tms);
				tms = System.currentTimeMillis();
				model.debug.x = (float)pos.x;
				model.debug.y = (float)pos.z;
				model.debug.z = (float)pos.y;

			}
		}, OptPos.class);
	}



}
