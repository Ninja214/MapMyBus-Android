package com.mapmybus.newmapmybus;

import com.crashlytics.android.Crashlytics;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.mapmybus.newmapmybus.model.Bus;
import com.mapmybus.newmapmybus.model.Department;
import com.mapmybus.newmapmybus.model.Organization;

public class SetupActivity extends Activity implements OnItemSelectedListener {
	private Spinner orgSpinner, departmentSpinner, busSpinner;
	private Button okButton;

	private List<Organization> orgList = new ArrayList<Organization>();
	private List<Department> depList = new ArrayList<Department>();
	private List<Bus> busList = new ArrayList<Bus>();

	private final static String ORGANIZATION_URL = "http://188.121.50.210:8080/MapMyBus/webresources/com.mapmybus.entities.organization";
	private final static String DEPARTMENT_URL = "http://188.121.50.210:8080/MapMyBus/webresources/com.mapmybus.entities.department";
	private final static String BUS_URL = "http://188.121.50.210:8080/MapMyBus/webresources/com.mapmybus.entities.bus";

	private final static String BY_ORG_ID = "/byOrganization?orgId=";
	private final static String BY_DEP_ID = "/byDepartment?depId=";

	private enum RequestType {
		ORG, DEP, BUS;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_setup);
		setProgressBarIndeterminateVisibility(true);

		String[] organization = new String[] { getString(R.string.org_select) };
		String[] department = new String[] { getString(R.string.dep_select) };
		String[] bus = new String[] { getString(R.string.bus_select) };

		orgSpinner = (Spinner) findViewById(R.id.orgSpinner);
		orgSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, organization));
		orgSpinner.setEnabled(false);
		orgSpinner.setOnItemSelectedListener(this);
		sendToServer(0, RequestType.ORG);

		departmentSpinner = (Spinner) findViewById(R.id.departmentSpinner);
		departmentSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, department));
		departmentSpinner.setOnItemSelectedListener(this);
		departmentSpinner.setEnabled(false);
		departmentSpinner.setVisibility(View.INVISIBLE);

		busSpinner = (Spinner) findViewById(R.id.busSpinner);
		busSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bus));
		busSpinner.setOnItemSelectedListener(this);
		busSpinner.setEnabled(false);
		busSpinner.setVisibility(View.INVISIBLE);

		okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(SetupActivity.this, CoordinatesActivity.class);
				i.putExtra("busname", ((Bus) busSpinner.getSelectedItem()).getName());
				i.putExtra("busid", ((Bus) busSpinner.getSelectedItem()).getId());
				i.putExtra("organizationname", ((Organization) orgSpinner.getSelectedItem()).getName());
				i.putExtra("departmentname", ((Department) departmentSpinner.getSelectedItem()).getName());
				startActivity(i);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.setup, menu);
		return true;
	}

	private void sendToServer(int idToSend, final RequestType reqType) {
		RequestQueue queue = Volley.newRequestQueue(this);

		String url = getRequestType(reqType, idToSend);

		JsonArrayRequest jsObjRequest = new JsonArrayRequest(url,
				new Response.Listener<JSONArray>() {
					@Override
					public void onResponse(JSONArray response) {
						switch (reqType) {
						case ORG:
							handleOrganizationRequest(response);
							setProgressBarIndeterminateVisibility(false);
							break;
						case DEP:
							handleDepartmentRequest(response);
							setProgressBarIndeterminateVisibility(false);
							break;
						case BUS:
							handleBusRequest(response);
							setProgressBarIndeterminateVisibility(false);
							break;
						default:
							break;
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						// Error here.
						// FIXME : Handle errors... Tell the user that something is wrong!
					}
				});

		queue.add(jsObjRequest);
		queue.start();
	}

	private void handleOrganizationRequest(JSONArray response) {
		try {
			Organization emptyItem = new Organization(0, "Select organization");
			orgList.clear();
			orgList.add(emptyItem);

			JSONObject object = null;
			Organization organization = null;

			for (int i = 0; i < response.length(); i++) {
				object = response.getJSONObject(i);
				organization = new Organization(object.getInt("id"), object.getString("name"));
				orgList.add(organization);
			}

			orgSpinner.setAdapter(new ArrayAdapter<Organization>(this, android.R.layout.simple_list_item_1, orgList));
			orgSpinner.setEnabled(true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void handleDepartmentRequest(JSONArray response) {
		try {
			Department emptyItem = new Department(0, "Select department");
			depList.clear();
			depList.add(emptyItem);

			JSONObject object = null;
			Department department = null;

			for (int i = 0; i < response.length(); i++) {
				object = response.getJSONObject(i);
				department = new Department(object.getInt("id"), object.getString("name"));
				depList.add(department);
			}

			departmentSpinner.setAdapter(new ArrayAdapter<Department>(this, android.R.layout.simple_list_item_1, depList));
			departmentSpinner.setEnabled(true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void handleBusRequest(JSONArray response) {
		try {
			Bus emptyItem = new Bus(0, "Select bus");
			busList.clear();
			busList.add(emptyItem);

			JSONObject object = null;
			Bus bus = null;

			for (int i = 0; i < response.length(); i++) {
				object = response.getJSONObject(i);
				bus = new Bus(object.getInt("id"), object.getString("name"));
				busList.add(bus);
			}

			busSpinner.setAdapter(new ArrayAdapter<Bus>(this, android.R.layout.simple_list_item_1, busList));
			busSpinner.setEnabled(true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String getRequestType(RequestType reqType, int id) {
		switch (reqType) {
		case BUS:
			return BUS_URL + (id == 0 ? "" : BY_DEP_ID + id);
		case DEP:
			return DEPARTMENT_URL + (id == 0 ? "" : BY_ORG_ID + id);
		case ORG:
			return ORGANIZATION_URL;
		default:
			return "";
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (position == 0) { // Fixing first selection...
			return;
		}

		switch (parent.getId()) {
		case R.id.orgSpinner:
			okButton.setVisibility(View.INVISIBLE);
			okButton.setEnabled(false);
			departmentSpinner.setEnabled(true);
			departmentSpinner.setVisibility(View.VISIBLE);
			busSpinner.setEnabled(false);
			Organization o = orgList.get(position);
			sendToServer(o.getId(), RequestType.DEP);
			break;
		case R.id.departmentSpinner:
			okButton.setVisibility(View.INVISIBLE);
			okButton.setEnabled(false);
			busSpinner.setEnabled(true);
			busSpinner.setVisibility(View.VISIBLE);
			Department d = (Department) parent.getSelectedItem();
			sendToServer(d.getId(), RequestType.BUS);
			break;
		case R.id.busSpinner:
			okButton.setVisibility(View.VISIBLE);
			okButton.setEnabled(true);
			break;
		default:
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}
}