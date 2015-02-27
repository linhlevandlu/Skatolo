/* 
 *  controlP5 is a processing gui library.
 * 
 * Copyright (C)  2006-2012 by Andreas Schlegel
 * Copyright (C)  2015 by Jeremy Laviole
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * 
 * 
 */
package fr.inria.controlP5.events;

import fr.inria.controlP5.ControlP5;
import fr.inria.controlP5.ControlP5Constants;
import fr.inria.controlP5.gui.Controller;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ControlBroadcaster handles all controller value changes and distributes them accordingly to its listeners. The ControlBroadcaster is
 * primarily for internal use only but can be accessed through an instance of the ControlP5 class. Instead of accessing the
 * ControlBroadcaster directly, use the convenience methods available from the ControlP5 class.
 * 
 * @see controlP5.ControlP5#getControlBroadcaster()
 */
public class ControlBroadcaster {

    	private ControlP5 cp5;
    
	private int controlEventType = ControlP5Constants.INVALID;

	private ControllerPlug controlEventPlug = null;
	private ControllerPlug controllerCallbackEventPlug = null;

	private String controllerCallbackEventMethod = "controlEvent";

	private ArrayList<ControlListener> controlListeners;
	private Map<CallbackListener, Controller<?>> controllerCallbackListeners;

	private static boolean setPrintStackTrace = true;
	private static boolean ignoreErrorMessage = false;

	private static Map<Class<?>, Field[]> fieldcache = new HashMap<Class<?>, Field[]>();
	private static Map<Class<?>, Method[]> methodcache = new HashMap<Class<?>, Method[]>();

	public boolean broadcast = true;

	public ControlBroadcaster(ControlP5 theControlP5) {
		cp5 = theControlP5;
		controlListeners = new ArrayList<ControlListener>();
		controllerCallbackListeners = new ConcurrentHashMap<CallbackListener, Controller<?>>();
		
                controlEventPlug =            checkObject(cp5.getObjectForIntrospection(), controllerCallbackEventMethod, new Class[] { ControlEvent.class });
                controllerCallbackEventPlug = checkObject(cp5.getObjectForIntrospection(), controllerCallbackEventMethod, new Class[] { CallbackEvent.class });
		
                if (controlEventPlug != null) {
			controlEventType = ControlP5Constants.METHOD;
		}
	}

	public ControlBroadcaster addListener(ControlListener... theListeners) {
		for (ControlListener l : theListeners) {
			controlListeners.add(l);
		}
		return this;
	}

	public ControlBroadcaster removeListener(ControlListener... theListeners) {
		for (ControlListener l : theListeners) {
			controlListeners.remove(l);
		}
		return this;
	}

	/**
	 * Returns a ControlListener by index
	 * 
	 * @param theIndex
	 * @return
	 */
	public ControlListener getListener(int theIndex) {
		if (theIndex < 0 || theIndex >= controlListeners.size()) {
			return null;
		}
		return controlListeners.get(theIndex);
	}

	/**
	 * Returns the size of the ControlListener list
	 * 
	 * @return
	 */
	public int listenerSize() {
		return controlListeners.size();
	}

	public ControlBroadcaster addCallback(CallbackListener... theListeners) {
		for (CallbackListener l : theListeners) {
			controllerCallbackListeners.put(l, new EmptyController());
		}
		return this;
	}

	public ControlBroadcaster addCallback(CallbackListener theListener) {
		controllerCallbackListeners.put(theListener, new EmptyController());
		return this;
	}

	/**
	 * Adds a CallbackListener for a list of controllers.
	 * 
	 * @param theListener
	 * @param theController
	 */
	public void addCallback(CallbackListener theListener, Controller<?>... theController) {
		for (Controller<?> c : theController) {
			controllerCallbackListeners.put(theListener, c);
		}
	}

	public ControlBroadcaster removeCallback(CallbackListener... theListeners) {
		for (CallbackListener c : theListeners) {
			controllerCallbackListeners.remove(c);
		}
		return this;
	}

	public ControlBroadcaster removeCallback(CallbackListener theListener) {
		controllerCallbackListeners.remove(theListener);
		return this;
	}

	/**
	 * Removes a CallbackListener for a particular controller
	 * 
	 * @param theController
	 */
	public ControlBroadcaster removeCallback(Controller<?>... theControllers) {
		for (Controller<?> c : theControllers) {
			for (Map.Entry<CallbackListener, Controller<?>> entry : controllerCallbackListeners.entrySet()) {
				if (c != null && entry.getValue().equals(c)) {
					controllerCallbackListeners.remove(entry.getKey());
				}
			}
		}
		return this;
	}

	public ControlBroadcaster plug(Object theObject, final String theControllerName, final String theTargetMethod) {
		plug(theObject, cp5.getController(theControllerName), theTargetMethod);
		return this;
	}

	public ControlBroadcaster plug(Object theObject, final Controller<?> theController, final String theTargetMethod) {
		if (theController != null) {
			ControllerPlug myControllerPlug = checkObject(theObject, theTargetMethod, ControlP5Constants.acceptClassList);
			if (myControllerPlug == null) {
				return this;
			} else {
				if (theController.checkControllerPlug(myControllerPlug) == false) {
					theController.addControllerPlug(myControllerPlug);
				}
				return this;
			}
		}
		return this;
	}

	static Field[] getFieldsFor(Class<?> theClass) {
		if (!fieldcache.containsKey(theClass)) {
			fieldcache.put(theClass, theClass.getDeclaredFields());
		}
		return fieldcache.get(theClass);
	}

	static Method[] getMethodFor(Class<?> theClass) {
		if (!methodcache.containsKey(theClass)) {
			methodcache.put(theClass, theClass.getDeclaredMethods());
		}
		return methodcache.get(theClass);
	}

	protected static ControllerPlug checkObject(final Object theObject, final String theTargetName, final Class<?>[] theAcceptClassList) {

		Class<?> myClass = theObject.getClass();

		Method[] myMethods = getMethodFor(myClass);

		for (int i = 0; i < myMethods.length; i++) {
			if ((myMethods[i].getName()).equals(theTargetName)) {

				if (myMethods[i].getParameterTypes().length == 1) {

					// hack to detect controlEvent(CallbackEvent) which is otherwise
					// overwritten by controlEvent(ControlEvent)
					if (theAcceptClassList.length == 1) {
						if (theAcceptClassList[0] == CallbackEvent.class) {
							ControllerPlug cp = new ControllerPlug(CallbackEvent.class, theObject, theTargetName, ControlP5Constants.EVENT, -1);
							if (cp.getMethod() == null) {
								return null;
							}
							return cp;
						}
					}
					if (myMethods[i].getParameterTypes()[0] == ControlP5Constants.controlEventClass) {
						return new ControllerPlug(ControlEvent.class, theObject, theTargetName, ControlP5Constants.EVENT, -1);
					}
					for (int j = 0; j < theAcceptClassList.length; j++) {
						if (myMethods[i].getParameterTypes()[0] == theAcceptClassList[j]) {
							return new ControllerPlug(theObject, theTargetName, ControlP5Constants.METHOD, j, theAcceptClassList);
						}
					}
				} else if (myMethods[i].getParameterTypes().length == 0) {
					return new ControllerPlug(theObject, theTargetName, ControlP5Constants.METHOD, -1, theAcceptClassList);
				}
				break;
			}
		}

		Field[] myFields = getFieldsFor(myClass);

		for (int i = 0; i < myFields.length; i++) {
			if ((myFields[i].getName()).equals(theTargetName)) {

				for (int j = 0; j < theAcceptClassList.length; j++) {
					if (myFields[i].getType() == theAcceptClassList[j]) {
						return new ControllerPlug(theObject, theTargetName, ControlP5Constants.FIELD, j, theAcceptClassList);
					}
				}
				break;
			}
		}
		return null;
	}

	public ControlBroadcaster broadcast(final ControlEvent theControlEvent, final int theType) {
		if (broadcast) {
			for (ControlListener cl : controlListeners) {
				cl.controlEvent(theControlEvent);
			}
			if (theControlEvent.isTab() == false && theControlEvent.isGroup() == false) {
				if (theControlEvent.getController().getControllerPlugList().size() > 0) {
					if (theType == ControlP5Constants.STRING) {
						for (ControllerPlug cp : theControlEvent.getController().getControllerPlugList()) {
							callTarget(cp, theControlEvent.getStringValue());
						}
					} else if (theType == ControlP5Constants.ARRAY) {

					} else {
						for (ControllerPlug cp : theControlEvent.getController().getControllerPlugList()) {
							if (cp.checkType(ControlP5Constants.EVENT)) {
								invokeMethod(cp.getObject(), cp.getMethod(), new Object[] { theControlEvent });
							} else {
								callTarget(cp, theControlEvent.getValue());
							}
						}
					}
				}
			}
			if (controlEventType == ControlP5Constants.METHOD) {
				invokeMethod(controlEventPlug.getObject(), controlEventPlug.getMethod(), new Object[] { theControlEvent });
			}
		}
		return this;
	}

	protected void callTarget(final ControllerPlug thePlug, final float theValue) {
		if (thePlug.checkType(ControlP5Constants.METHOD)) {

			invokeMethod(thePlug.getObject(), thePlug.getMethod(), thePlug.getMethodParameter(theValue));
		} else if (thePlug.checkType(ControlP5Constants.FIELD)) {
			invokeField(thePlug.getObject(), thePlug.getField(), thePlug.getFieldParameter(theValue));
		}
	}

	protected void callTarget(final ControllerPlug thePlug, final String theValue) {
		if (thePlug.checkType(ControlP5Constants.METHOD)) {
			invokeMethod(thePlug.getObject(), thePlug.getMethod(), new Object[] { theValue });
		} else if (thePlug.checkType(ControlP5Constants.FIELD)) {
			invokeField(thePlug.getObject(), thePlug.getField(), theValue);
		}
	}

	private void invokeField(final Object theObject, final Field theField, final Object theParam) {
		try {
			theField.set(theObject, theParam);
		} catch (IllegalAccessException e) {
			ControlP5.logger().warning(e.toString());
		}
	}

	private void invokeMethod(final Object theObject, final Method theMethod, final Object[] theParam) {
		try {
			if (theParam[0] == null) {
				theMethod.invoke(theObject, new Object[0]);
			} else {
				theMethod.invoke(theObject, theParam);
			}
		} catch (IllegalArgumentException e) {
			ControlP5.logger().warning(e.toString());
			/**
			 * TODO thrown when plugging a String method/field.
			 */
		} catch (IllegalAccessException e) {
			printMethodError(theMethod, e);
		} catch (InvocationTargetException e) {
			printMethodError(theMethod, e);
		} catch (NullPointerException e) {
			printMethodError(theMethod, e);
		}

	}

	public String getEventMethod() {
		return controllerCallbackEventMethod;
	}

	public void invokeAction(CallbackEvent theEvent) {
		boolean invoke;
		for (Map.Entry<CallbackListener, Controller<?>> entry : controllerCallbackListeners.entrySet()) {
			invoke = (entry.getValue().getClass().equals(EmptyController.class)) ? true : (entry.getValue().equals(theEvent.getController())) ? true : false;
			if (invoke) {
				entry.getKey().controlEvent(theEvent);
			}
		}

		if (controllerCallbackEventPlug != null) {
			invokeMethod(cp5.getObjectForIntrospection(), controllerCallbackEventPlug.getMethod(), new Object[] { theEvent });
		}
	}

	private void printMethodError(Method theMethod, Exception theException) {
		if (!ignoreErrorMessage) {
			ControlP5.logger().severe(
					"An error occured while forwarding a Controller event, please check your code at " + theMethod.getName() + (!setPrintStackTrace ? " " + "exception:  " + theException : ""));
			if (setPrintStackTrace) {
				theException.printStackTrace();
			}
		}
	}

	public static void ignoreErrorMessage(boolean theFlag) {
		ignoreErrorMessage = theFlag;
	}

	public static void setPrintStackTrace(boolean theFlag) {
		setPrintStackTrace = theFlag;
	}

	private class EmptyController extends Controller<EmptyController> {

		protected EmptyController() {
			this(0, 0);
		}

		protected EmptyController(int theX, int theY) {
			super("empty" + ((int) (Math.random() * 1000000)), theX, theY);
			// TODO Auto-generated constructor stub
		}

		@Override public EmptyController setValue(float theValue) {
			// TODO Auto-generated method stub
			return this;
		}

	}

	/**
	 * @exclude
	 */
	@Deprecated public void plug(final String theControllerName, final String theTargetMethod) {
		plug(cp5.getObjectForIntrospection(), theControllerName, theTargetMethod);
	}
}