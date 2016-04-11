/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight;

import java.io.IOException;
import java.util.Map;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.lquac.msg_rc_channels_override;

import com.comino.flight.control.ControlProperties;
import com.comino.flight.control.integration.AnalysisIntegration;
import com.comino.flight.control.integration.SlamTest;
import com.comino.flight.panel.control.FlightControlPanel;
import com.comino.flight.tabs.FlightTabs;
import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.mav.control.impl.MAVSimController;
import com.comino.mav.control.impl.MAVUdpController;
import com.comino.model.file.FileHandler;
import com.comino.msp.log.MSPLogger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

	private static IMAVController control = null;

	private static FlightControlPanel controlpanel = null;

	private Stage primaryStage;
	private BorderPane rootLayout;

	@FXML
	private MenuItem m_close;

	@FXML
	private MenuItem m_import;

	@FXML
	private MenuItem m_export;

	@FXML
	private MenuItem m_px4log;

	@FXML
	private MenuBar menubar;


	public MainApp() {
		super();
	}

	@Override
	public void start(Stage primaryStage) {

		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("MAVGCL Analysis");

		String peerAddress = null;
        String proxy = null;

		Map<String,String> args = getParameters().getNamed();

		if(args.size()> 0) {
			peerAddress  = args.get("peerAddress");
			proxy = args.get("proxy");
		}

		if(peerAddress ==null) {
			control = new MAVSimController();
			control.connect();
		}
		else {
			if(peerAddress.contains("127.0") || peerAddress.contains("localhost")) {
				if(proxy==null)
				    control = new MAVUdpController(peerAddress,14556,14550, true);
				else
					control = new MAVUdpController(peerAddress,14558,14550, true);

			}
			else {
				control = new MAVUdpController(peerAddress,14555,14550, false);
			}
		}

		SlamTest slam = new SlamTest();
		slam.registerFunction(control);
		MSPLogger.getInstance(control);
		ControlProperties.getInstance(control);

		FileHandler.getInstance(primaryStage,control);

		initRootLayout();
		showMAVGCLApplication();

	}

	@Override
	public void stop() throws Exception {
		control.close();
		super.stop();
		System.exit(0);
	}


	public static void main(String[] args) {
		launch(args);
	}

	public void initRootLayout() {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			// Show the scene containing the root layout.
			Scene scene = new Scene(rootLayout);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());


	        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
	            @Override
	            public void handle(KeyEvent event) {
					if(control.sendMAVLinkCmd(MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM, 0, 21196 ))
						MSPLogger.getInstance().writeLocalMsg("EMERGENCY: User requested to switch off motors");
	            }
	        });

			primaryStage.setScene(scene);
			primaryStage.show();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void initialize() {

		menubar.setUseSystemMenuBar(true);

		m_import.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileHandler.getInstance().fileImport();
				controlpanel.getRecordControl().refreshCharts();
			}

		});

		m_px4log.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileHandler.getInstance().fileImportPX4Log();
				controlpanel.getRecordControl().refreshCharts();

			}

		});

		m_export.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(control.getCollector().getModelList().size()>0)
					FileHandler.getInstance().fileExport();
			}
		});
	}


	public void showMAVGCLApplication() {

		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("tabs/MAVGCL2.fxml"));
			AnchorPane flightPane = (AnchorPane) loader.load();

			// Set person overview into the center of root layout.
			rootLayout.setCenter(flightPane);
			BorderPane.setAlignment(flightPane, Pos.TOP_LEFT);;


			StatusLineWidget statusline = new StatusLineWidget();
			rootLayout.setBottom(statusline);

			statusline.setup(control);

			controlpanel = new FlightControlPanel();
			rootLayout.setLeft(controlpanel);
			controlpanel.setup(control);


			if(!control.isConnected())
				control.connect();

			FlightTabs fvController = loader.getController();
			fvController.setup(controlpanel,statusline, control);
			fvController.setPrefHeight(820);
		//	fvController.prefHeightProperty().bind(rootLayout.heightProperty());



		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
