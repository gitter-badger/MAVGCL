package com.comino.flight.control.integration;

import com.comino.mav.control.IMAVController;
import com.comino.mq.bus.IMWMessageBusListener;
import com.comino.mq.bus.MWMessageBus;
import com.comino.mq.tests.OptPos;

public class SlamTest {

	private OptPos pos = null;

	public SlamTest() {

		MWMessageBus bus1 = new MWMessageBus(1,"127.0.0.1");

		bus1.addListener(new IMWMessageBusListener() {

			@Override
			public void received(Object o) {

				pos = (OptPos)o;

			}
		}, OptPos.class);
	}


	public void registerFunction(IMAVController control) {

		/*  EXAMPLE for Debugging values */

		control.getCollector().addInjectionListener(model -> {
			model.debug.x = (float)pos.x;
			model.debug.y = (float)pos.y;
			model.debug.z = (float)pos.z;
		});

	}

}
