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
package fr.inria.controlP5.gui.group;

import fr.inria.controlP5.gui.CColor;
import fr.inria.controlP5.gui.CDrawable;
import fr.inria.controlP5.Hacks;
import fr.inria.controlP5.gui.Canvas;
import fr.inria.controlP5.events.ControlEvent;
import fr.inria.controlP5.ControlFont;
import fr.inria.controlP5.events.ControlListener;
import fr.inria.controlP5.ControlP5;
import fr.inria.controlP5.ControlP5Constants;
import fr.inria.controlP5.gui.ControlWindow;
import fr.inria.controlP5.gui.Controller;
import fr.inria.controlP5.gui.ControllerInterface;
import fr.inria.controlP5.gui.ControllerList;
import fr.inria.controlP5.file.ControllerProperty;
import fr.inria.controlP5.gui.Label;
import fr.inria.controlP5.gui.group.Tab;
import fr.inria.controlP5.gui.controllers.Button;
import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.KeyEvent;

/**
 * ControllerGroup is an abstract class and is extended by class ControlGroup,
 * Tab, or the ListBox.
 *
 */
public abstract class ControllerGroup<T> implements ControllerInterface<T>, ControlP5Constants, ControlListener {

    protected ControllerList controllers;
    protected List<ControlListener> _myControlListener;

    protected ControlP5 cp5;
    protected ControllerGroup<?> _myParent;

    protected String _myName;
    protected int _myId = -1;

    protected PVector position;
    protected PVector positionBuffer;
    protected PVector absolutePosition;

    //	protected ControlWindow _myControlWindow;
    private final CColor color = new CColor();
    protected boolean isMousePressed = false;

    // only applies to the area of the title bar of a group
    protected boolean isInside = false;

    // applies to the area including controllers, currently only supported for listbox
    protected boolean isArrowVisible = true;
    protected boolean isInsideGroup = false;
    protected boolean isVisible = true;
    protected boolean isOpen = true;
    protected boolean isBarVisible = true;

    protected boolean isMoveable = true;

    protected Button closeButton;

    protected Label _myLabel;
    protected Label _myValueLabel;

    protected int _myWidth = 99;

    protected int _myHeight = 9;

    protected boolean isUpdate;

    protected List<Canvas> _myCanvas;

    protected float _myValue;

    protected String _myStringValue;

    protected float[] _myArrayValue;

    protected boolean isCollapse = true;

    protected int _myPickingColor = 0x6600ffff;

    public PVector autoPosition = new PVector(10, 30, 0);

    public float tempAutoPositionHeight = 0;

    public float autoPositionOffsetX = 10;

    private String _myAddress = "";

    private boolean mouseover;

    protected final T me;

    /**
     * Convenience constructor to extend ControllerGroup.
     *
     * @example use/ControlP5extendController
     * @param theControlP5
     * @param theName
     */
    public ControllerGroup(ControlP5 theControlP5, String theName) {
        this(theControlP5, theControlP5.getDefaultTab(), theName, 0, 0);
        theControlP5.register(theControlP5.getObjectForIntrospection(), theName, this);
    }

    /**
     *
     * @param theControlP5 ControlP5
     * @param theParent ControllerGroup
     * @param theName String
     * @param theX float
     * @param theY float
     * @exclude
     */
    public ControllerGroup(ControlP5 theControlP5, ControllerGroup<?> theParent, String theName, float theX, float theY) {
        position = new PVector(theX, theY, 0);
        cp5 = theControlP5;
        me = (T) this;
        color.set((theParent == null) ? cp5.getColor() : theParent.getColor());
        _myName = theName;
        controllers = new ControllerList();
        _myCanvas = new ArrayList<Canvas>();
        _myControlListener = new ArrayList<ControlListener>();
        _myLabel = new Label(cp5, _myName);
        _myLabel.setText(_myName);
        _myLabel.setColor(color.getCaptionLabel());
        _myLabel.align(LEFT, TOP);
        setParent((theParent == null) ? this : theParent);
    }

    protected ControllerGroup(int theX, int theY) {
        position = new PVector(theX, theY, 0);
        me = (T) this;
        controllers = new ControllerList();
        _myCanvas = new ArrayList<Canvas>();
    }

    /**
     * @exclude {@inheritDoc}
     */
    public void init() {
    }

    /**
     * @exclude {@inheritDoc}
     */
    @Override
    public ControllerInterface<?> getParent() {
        return _myParent;
    }

    void setParent(ControllerGroup<?> theParent) {

        if (_myParent != null && _myParent != this) {
            _myParent.remove(this);
        }

        _myParent = theParent;

        if (_myParent != this) {
            _myParent.add(this);
        }

        absolutePosition = new PVector(position.x, position.y);

        absolutePosition.add(_myParent.absolutePosition);

        positionBuffer = new PVector(position.x, position.y);

        if (cp5.getWindow() != null) {
            setMouseOver(false);
        }
    }

    /**
     * @param theGroup ControllerGroup
     * @return ControllerGroup
     */
    public final T setGroup(ControllerGroup<?> theGroup) {
        setParent(theGroup);
        return me;
    }

    /**
     * @param theName String
     * @return ControllerGroup
     */
    public final T setGroup(String theName) {
        setParent(cp5.getGroup(theName));
        return me;
    }

    /**
     * @param theGroup ControlGroup
     * @param theTab Tab
     * @param theControlWindow ControlWindow
     * @return ControllerGroup
     */
    public final T moveTo(ControllerGroup<?> theGroup, Tab theTab, ControlWindow theControlWindow) {
        if (theGroup != null) {
            setGroup(theGroup);
            return me;
        }

        if (theControlWindow == null) {
            theControlWindow = cp5.controlWindow;
        }

        setTab(theControlWindow, theTab.getName());
        return me;
    }

    public final T moveTo(ControllerGroup<?> theGroup) {
        moveTo(theGroup, null, null);
        return me;
    }

    public final T moveTo(Tab theTab) {
        moveTo(null, theTab, theTab.getWindow());
        return me;
    }

    public T moveTo(PApplet thePApplet) {
        moveTo(cp5.controlWindow);
        return me;
    }

    public T moveTo(ControlWindow theControlWindow) {
        moveTo(null, theControlWindow.getTab("default"), theControlWindow);
        return me;
    }

    public final T moveTo(String theTabName) {
        moveTo(null, cp5.controlWindow.getTab(theTabName), cp5.controlWindow);
        return me;
    }

    public final T moveTo(String theTabName, ControlWindow theControlWindow) {
        moveTo(null, theControlWindow.getTab(theTabName), theControlWindow);
        return me;
    }

    public final T moveTo(ControlWindow theControlWindow, String theTabName) {
        moveTo(null, theControlWindow.getTab(theTabName), theControlWindow);
        return me;
    }

    public final T moveTo(Tab theTab, ControlWindow theControlWindow) {
        moveTo(null, theTab, theControlWindow);
        return me;
    }

    /**
     * @param theName String
     * @return ControllerGroup
     */
    public final T setTab(String theName) {
        setParent(cp5.getTab(theName));
        return me;
    }

    public final T setTab(ControlWindow theWindow, String theName) {
        setParent(cp5.getTab(theWindow, theName));
        return me;
    }

    /**
     * @param theTab Tab
     * @return ControllerGroup
     */
    public final T setTab(Tab theTab) {
        setParent(theTab);
        return me;
    }

    /**
     * @return Tab
     */
    public Tab getTab() {
        if (this instanceof Tab) {
            return (Tab) this;
        }
        if (_myParent instanceof Tab) {
            return (Tab) _myParent;
        }
        return _myParent.getTab();
    }

    public void updateFont(ControlFont theControlFont) {
        _myLabel.updateFont(theControlFont);
        if (_myValueLabel != null) {
            _myValueLabel.updateFont(theControlFont);
        }
        for (int i = 0; i < controllers.size(); i++) {
            if (controllers.get(i) instanceof Controller<?>) {
                ((Controller<?>) controllers.get(i)).updateFont(theControlFont);
            } else {
                ((ControllerGroup<?>) controllers.get(i)).updateFont(theControlFont);
            }
        }
    }

    public PVector getAbsolutePosition() {
        return new PVector(absolutePosition.x, absolutePosition.y);
    }

    /**
     * @exclude {@inheritDoc}
     */
    public T setAbsolutePosition(PVector thePVector) {
        absolutePosition.set(thePVector.x, thePVector.y, thePVector.z);
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public PVector getPosition() {
        return new PVector(position.x, position.y);
    }

    /**
     * set the position of this controller.
     *
     * @param theX float
     * @param theY float
     */
    public T setPosition(float theX, float theY) {
        position.set((int) theX, (int) theY, 0);
        positionBuffer.set(position);
        updateAbsolutePosition();
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setPosition(PVector thePVector) {
        setPosition(thePVector.x, thePVector.y);
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T updateAbsolutePosition() {
        absolutePosition.set(position);
        absolutePosition.add(_myParent.getAbsolutePosition());
        for (int i = 0; i < controllers.size(); i++) {
            controllers.get(i).updateAbsolutePosition();
        }
        return me;
    }

    /**
     * @exclude {@inheritDoc}
     */
    public void continuousUpdateEvents() {
        if (controllers.size() <= 0) {
            return;
        }
        for (int i = controllers.size() - 1; i >= 0; i--) {
            ((ControllerInterface<?>) controllers.get(i)).continuousUpdateEvents();
        }
    }

    /**
     * @exclude @return ControllerGroup
     */
    public T update() {
        if (controllers.size() <= 0) {
            return me;
        }
        for (int i = controllers.size() - 1; i >= 0; i--) {
            if (((ControllerInterface<?>) controllers.get(i)).isUpdate()) {
                ((ControllerInterface<?>) controllers.get(i)).update();
            }
        }
        return me;
    }

    /**
     * enables or disables the update function of a controller.
     *
     * @param theFlag boolean
     * @return ControllerGroup
     */
    @Override
    public T setUpdate(boolean theFlag) {
        isUpdate = theFlag;
        for (int i = 0; i < controllers.size(); i++) {
            ((ControllerInterface<?>) controllers.get(i)).setUpdate(theFlag);
        }
        return me;
    }

    /**
     * checks the update status of a controller.
     *
     * @return boolean
     */
    public boolean isUpdate() {
        return isUpdate;
    }

    /**
     * @exclude {@inheritDoc}
     */
    public T updateEvents() {
        if (isOpen) {
            for (int i = controllers.size() - 1; i >= 0; i--) {
                ((ControllerInterface<?>) controllers.get(i)).updateEvents();
            }
        }
        if (isVisible) {
            if ((isMousePressed == cp5.getWindow().mouselock)) {
                if (isMousePressed && cp5.isAltDown() && isMoveable) {
                    if (!cp5.isMoveable) {
                        positionBuffer.x += cp5.getWindow().getMouseX() - cp5.getWindow().getPMouseX();
                        positionBuffer.y += cp5.getWindow().getMouseY() - cp5.getWindow().getPMouseY();
                        if (cp5.isShiftDown()) {
                            position.x = ((int) (positionBuffer.x) / 10) * 10;
                            position.y = ((int) (positionBuffer.y) / 10) * 10;
                        } else {
                            position.set(positionBuffer);
                        }
                        updateAbsolutePosition();
                    }
                } else {
                    if (isInside) {
                        setMouseOver(true);
                    }
                    if (inside()) {
                        if (!isInside) {
                            isInside = true;
                            onEnter();
                            setMouseOver(true);
                        }
                    } else {
                        if (isInside && !isMousePressed) {
                            onLeave();
                            isInside = false;
                            setMouseOver(false);
                        }
                    }
                }
            }
        }
        return me;
    }

    /**
     * @exclude {@inheritDoc}
     */
    public T updateInternalEvents(PApplet theApplet) {
        return me;
    }

    /**
     * {@inheritDoc}
     *
     * @return boolean
     */
    public boolean isMouseOver() {
        mouseover = isInside || isInsideGroup || !isBarVisible;
        return mouseover;
    }

    public T setMouseOver(boolean theFlag) {

        mouseover = (!isBarVisible) ? false : theFlag;

        if (!mouseover) {
            isInside = false;
            isInsideGroup = false;
            cp5.getWindow().removeMouseOverFor(this);
            for (int i = controllers.size() - 1; i >= 0; i--) {
                controllers.get(i).setMouseOver(false);
            }
        } else {
			// TODO since inside can be either isInside or isInsideGroup, there are 2 options here,
            // which i am not sure how to handle them yet.
            cp5.getWindow().setMouseOverController(this);
        }
        return me;
    }

    /**
     *
     * @exclude @param graphics graphics context
     * @param theApplet PApplet for events
     */
    public final void draw(PApplet theApplet, PGraphics graphics) {
        if (isVisible) {
            graphics.pushMatrix();
            graphics.translate(position.x, position.y);
            preDraw(graphics);
            updateControllers(theApplet);
            drawControllers(graphics);
            postDraw(graphics);
            if (_myValueLabel != null) {
                _myValueLabel.draw(graphics, 2, 2, this);
            }
            graphics.popMatrix();
        }
    }

    /**
     *
     * @exclude @param graphics graphics context
     */
    public final void draw(PGraphics graphics) {
        if (isVisible) {
            graphics.pushMatrix();
            graphics.translate(position.x, position.y);
            preDraw(graphics);
                        // TODO: fixme -> Find the Applet to forward events ? 
            // Push outside the draw method !
            drawControllers(graphics);
            postDraw(graphics);
            if (_myValueLabel != null) {
                _myValueLabel.draw(graphics, 2, 2, this);
            }
            graphics.popMatrix();
        }
    }

// Legacy, update & draw at the same place. 
    protected void drawControllers(PGraphics graphics) {
        if (isOpen) {
            for (Canvas cc : _myCanvas) {
                if (cc.mode() == Canvas.PRE) {
                    cc.draw(graphics);
                }
            }
            for (ControllerInterface<?> ci : controllers.get()) {
                if (ci.isVisible()) {
                    ci.draw(graphics);

                    // TODO: This shoud NOT be HERE !!
                    ci.updateInternalEvents(cp5.getPApplet());
                }
            }

            for (CDrawable cd : controllers.getDrawables()) {
                cd.draw(graphics);
            }

            for (Canvas cc : _myCanvas) {
                if (cc.mode() == Canvas.POST) {
                    cc.draw(graphics);
                }
            }
        }
    }

    protected void updateControllers(PApplet theApplet) {
        if (isOpen) {
            for (ControllerInterface<?> ci : controllers.get()) {
                if (ci.isVisible()) {
                    ci.updateInternalEvents(theApplet);
                }
            }

        }
    }

    protected void preDraw(PGraphics graphics) {
    }

    protected void postDraw(PGraphics graphics) {
    }

    /**
     * Adds a canvas to a controllerGroup such as a tab or group. Use
     * processing's draw methods to add visual content.
     *
     * @param theCanvas
     * @return Canvas
     */
    public Canvas addCanvas(Canvas theCanvas) {
        _myCanvas.add(theCanvas);
        theCanvas.setup(cp5.getPApplet());
        return theCanvas;
    }

    /**
     * Removes a canvas from a controller group.
     *
     * @param theCanvas
     * @return ControllerGroup
     */
    public T removeCanvas(Canvas theCanvas) {
        _myCanvas.remove(theCanvas);
        return me;
    }

    /**
     * Adds a controller to the group, but use Controller.setGroup() instead.
     *
     * @param theElement ControllerInterface
     * @return ControllerGroup
     */
    public T add(ControllerInterface<?> theElement) {
        controllers.add(theElement);
        return me;
    }

    @Override
    public T bringToFront() {
        return bringToFront(this);
    }

    @Override
    public T bringToFront(ControllerInterface<?> theController) {
        if (_myParent instanceof Tab) {
            moveTo((Tab) _myParent);
        } else {
            _myParent.bringToFront(theController);
        }
        if (theController != this) {
            if (controllers.get().contains(theController)) {
                controllers.remove(theController);
                controllers.add(theController);
            }
        }
        return me;
    }

    /**
     * Removes a controller from the group, but use Controller.setGroup()
     * instead.
     *
     * @param theElement ControllerInterface
     * @return ControllerGroup
     */
    public T remove(ControllerInterface<?> theElement) {
        if (theElement != null) {
            theElement.setMouseOver(false);
        }
        controllers.remove(theElement);
        return me;
    }

    /**
     * @param theElement CDrawable
     * @return ControllerGroup
     */
    public T addDrawable(CDrawable theElement) {
        controllers.addDrawable(theElement);
        return me;
    }

    /**
     * @param theElement CDrawable
     * @return ControllerGroup
     */
    public T remove(CDrawable theElement) {
        controllers.removeDrawable(theElement);
        return me;
    }

    /**
     * removes the group from controlP5.
     */
    public void remove() {
        cp5.getWindow().removeMouseOverFor(this);
        if (_myParent != null) {
            _myParent.remove(this);
        }
        if (cp5 != null) {
            cp5.remove(this);
        }

        for (int i = controllers.size() - 1; i >= 0; i--) {
            controllers.get(i).remove();
        }
        controllers.clear();
        controllers.clearDrawable();
        controllers = new ControllerList();
        if (this instanceof Tab) {
            cp5.getWindow().removeTab((Tab) this);
        }
    }

    /**
     * @return String
     */
    public String getName() {
        return _myName;
    }

    /**
     * {@inheritDoc}
     */
    public String getAddress() {
        return _myAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T setAddress(String theAddress) {
        if (_myAddress.length() == 0) {
            _myAddress = theAddress;
        }
        return me;
    }

    /**
     * @return ControlWindow
     */
    public ControlWindow getWindow() {
        return cp5.getWindow();
    }

    /**
     * @exclude @param theEvent KeyEvent
     */
    public void keyEvent(KeyEvent theEvent) {
        for (int i = 0; i < controllers.size(); i++) {
            ((ControllerInterface<?>) controllers.get(i)).keyEvent(theEvent);
        }
    }

    /**
     * @exclude @param theStatus boolean
     * @return boolean
     */
    public boolean setMousePressed(boolean theStatus) {
        if (!isVisible) {
            return false;
        }
        for (int i = controllers.size() - 1; i >= 0; i--) {
            if (((ControllerInterface<?>) controllers.get(i)).setMousePressed(theStatus)) {
                return true;
            }
        }
        if (theStatus == true) {
            if (isInside) {
                isMousePressed = true;
                mousePressed();
                return true;
            }
        } else {
            if (isMousePressed == true) {
                isMousePressed = false;
                mouseReleased();
            }
        }
        return false;
    }

    protected void mousePressed() {
    }

    public void mouseReleased() {
    }

    protected void onEnter() {
    }

    protected void onLeave() {
    }

    public void onScroll(int theAmount) {
    }

    /**
     * {@inheritDoc}
     */
    public T setId(int theId) {
        _myId = theId;
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public int getId() {
        return _myId;
    }

    /**
     * {@inheritDoc}
     */
    public T setColor(CColor theColor) {
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColor(theColor);
        }
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setColorActive(int theColor) {
        color.setActive(theColor);
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColorActive(theColor);
        }
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setColorForeground(int theColor) {
        color.setForeground(theColor);
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColorForeground(theColor);
        }
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setColorBackground(int theColor) {
        color.setBackground(theColor);
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColorBackground(theColor);
        }
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setColorLabel(int theColor) {
        color.setCaptionLabel(theColor);
        if (_myLabel != null) {
            _myLabel.setColor(color.getCaptionLabel());
        }
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColorLabel(theColor);
        }
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T setColorValue(int theColor) {
        color.setValueLabel(theColor);
        if (_myValueLabel != null) {
            _myValueLabel.setColor(color.getValueLabel());
        }
        for (ControllerInterface<?> ci : controllers.get()) {
            ci.setColorValue(theColor);
        }
        return me;
    }

    /**
     * @param theLabel String
     * @return ControllerGroup
     */
    public T setLabel(String theLabel) {
        _myLabel.set(theLabel);
        return me;
    }

    /**
     * @return boolean
     */
    public boolean isVisible() {
        if (_myParent != null && _myParent != this) {
            if (getParent().isVisible() == false) {
                return false;
            }
        }
        return isVisible;
    }

    /**
     * @param theFlag boolean
     * @return ControllerGroup
     */
    public T setVisible(boolean theFlag) {
        isVisible = theFlag;
        return me;
    }

    public T hide() {
        isVisible = false;
        return me;
    }

    public T show() {
        isVisible = true;
        return me;
    }

    /**
     * set the moveable status of the group, when false, the group can't be
     * moved.
     *
     * @param theFlag boolean
     * @return ControllerGroup
     */
    public T setMoveable(boolean theFlag) {
        isMoveable = theFlag;
        return me;
    }

    public boolean isMoveable() {
        return isMoveable;
    }

    public T setOpen(boolean theFlag) {
        isOpen = theFlag;
        return me;
    }

    /**
     * @return boolean
     */
    public boolean isOpen() {
        return isOpen;
    }

    public T open() {
        setOpen(true);
        return me;
    }

    public T close() {
        setOpen(false);
        return me;
    }

    /**
     * TODO redesign or deprecate remove the close button.
     */
    public T removeCloseButton() {
        if (closeButton == null) {
            closeButton.remove();
        }
        closeButton = null;
        return me;
    }

    public T setTitle(String theTitle) {
        getCaptionLabel().set(theTitle);
        return me;
    }

    public T hideBar() {
        isBarVisible = false;
        return me;
    }

    public T showBar() {
        isBarVisible = true;
        return me;
    }

    /**
     * @return boolean
     */
    public boolean isBarVisible() {
        return isBarVisible;
    }

    /**
     * @return ControlGroup
     */
    public T hideArrow() {
        isArrowVisible = false;
        return me;
    }

    /**
     * @return ControlGroup
     */
    public T showArrow() {
        isArrowVisible = true;
        return me;
    }

    /**
     * TODO redesign or deprecate add a close button to the controlbar of this
     * controlGroup.
     */
    public T addCloseButton() {
        if (closeButton == null) {
            closeButton = new Button(cp5, this, getName() + "close", 1, _myWidth + 1, -10, 12, 9);
            closeButton.setCaptionLabel("X");
            closeButton.addListener(this);
        }
        return me;
    }

    /**
     * @exclude {@inheritDoc}
     */
    public int getPickingColor() {
        return _myPickingColor;
    }

    /**
     * {@inheritDoc}
     */
    public CColor getColor() {
        return color;
    }

    /**
     * {@inheritDoc}
     */
    public T setValue(float theValue) {
        _myValue = theValue;
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public float getValue() {
        return _myValue;
    }

    /**
     * {@inheritDoc}
     */
    public String getStringValue() {
        return _myStringValue;
    }

    /**
     * {@inheritDoc}
     */
    public T setStringValue(String theValue) {
        _myStringValue = theValue;
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public float[] getArrayValue() {
        return _myArrayValue;
    }

    /**
     * @param theIndex
     * @return float
     */
    public float getArrayValue(int theIndex) {
        if (theIndex >= 0 && theIndex < _myArrayValue.length) {
            return _myArrayValue[theIndex];
        } else {
            return Float.NaN;
        }
    }

    /**
     *
     * @param theIndex
     * @param theValue
     * @return Controller
     */
    public T setArrayValue(int theIndex, float theValue) {
        if (theIndex >= 0 && theIndex < _myArrayValue.length) {
            _myArrayValue[theIndex] = theValue;
        }
        return me;
    }

    /**
     * @param theArray
     * @return ControllerGroup
     */
    public T setArrayValue(float[] theArray) {
        _myArrayValue = theArray;
        return me;
    }

    public Controller<?> getController(String theController) {
        return cp5.getController(theController);
    }

    public T setCaptionLabel(String theValue) {
        getCaptionLabel().set(theValue);
        return me;
    }

    public Label getCaptionLabel() {
        return _myLabel;
    }

    public Label getValueLabel() {
        return _myValueLabel;
    }

    /**
     * @return ControllerGroup
     */
    public T enableCollapse() {
        isCollapse = true;
        return me;
    }

    /**
     * @return ControllerGroup
     */
    public T disableCollapse() {
        isCollapse = false;
        return me;
    }

    public boolean isCollapse() {
        return isCollapse;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidth() {
        return _myWidth;
    }

    /**
     * {@inheritDoc}
     */
    public int getHeight() {
        return _myHeight;
    }

    /**
     * @param theWidth
     * @return ControllerGroup
     */
    public T setWidth(int theWidth) {
        _myWidth = theWidth;
        return me;
    }

    /**
     * @param theHeight
     * @return ControllerGroup
     */
    public T setHeight(int theHeight) {
        _myHeight = theHeight;
        return me;
    }

    public T setSize(int theWidth, int theHeight) {
        setWidth(theWidth);
        // setHeight(theHeight) will set the Height of the bar therefore will not be used here.
        return me;
    }

    protected boolean inside() {
        return (cp5.getWindow().getMouseX() > position.x + _myParent.absolutePosition.x && cp5.getWindow().getMouseX() < position.x + _myParent.absolutePosition.x + _myWidth
                && cp5.getWindow().getMouseY() > position.y + _myParent.absolutePosition.y - _myHeight && cp5.getWindow().getMouseY() < position.y + _myParent.absolutePosition.y);
    }

    /**
     * {@inheritDoc}
     */
    public ControllerProperty getProperty(String thePropertyName) {
        return cp5.getProperties().getProperty(this, thePropertyName);
    }

    /**
     * {@inheritDoc}
     */
    public ControllerProperty getProperty(String theSetter, String theGetter) {
        return cp5.getProperties().getProperty(this, theSetter, theGetter);
    }

    /**
     * {@inheritDoc}
     */
    public T registerProperty(String thePropertyName) {
        cp5.getProperties().register(this, thePropertyName);
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T registerProperty(String theSetter, String theGetter) {
        cp5.getProperties().register(this, theSetter, theGetter);
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T removeProperty(String thePropertyName) {
        cp5.getProperties().remove(this, thePropertyName);
        return me;
    }

    /**
     * {@inheritDoc}
     */
    public T removeProperty(String theSetter, String theGetter) {
        cp5.getProperties().remove(this, theSetter, theGetter);
        return me;
    }

    public void controlEvent(ControlEvent theEvent) {
    }

    /**
     * @see controlP5.ControlListener
     * @param theListener ControlListener
     * @return Controller
     */
    public T addListener(final ControlListener theListener) {
        _myControlListener.add(theListener);
        return me;
    }

    /**
     * @see controlP5.ControlListener
     * @param theListener ControlListener
     * @return Controller
     */
    public T removeListener(final ControlListener theListener) {
        _myControlListener.remove(theListener);
        return me;
    }

    /**
     * @return int
     */
    public int listenerSize() {
        return _myControlListener.size();
    }

    @Override
    public String toString() {
        return getName() + " [" + getClass().getSimpleName() + "]";
    }

    public String getInfo() {
        return "type:\tControllerGroup" + "\nname:\t" + _myName + "\n" + "label:\t" + _myLabel.getText() + "\n" + "id:\t" + _myId + "\n" + "value:\t" + _myValue + "\n" + "arrayvalue:\t"
                + Hacks.arrayToString(_myArrayValue) + "\n" + "position:\t" + position + "\n" + "absolute:\t" + absolutePosition + "\n" + "width:\t" + getWidth() + "\n" + "height:\t" + getHeight()
                + "\n" + "color:\t" + getColor() + "\n" + "visible:\t" + isVisible + "\n" + "moveable:\t" + isMoveable + "\n";
    }

    /**
     * @exclude
     */
    @Deprecated
    public PVector absolutePosition() {
        return getAbsolutePosition();
    }

    /**
     * @exclude
     */
    @Deprecated
    public PVector position() {
        return getPosition();
    }

    /**
     * @exclude
     */
    @Deprecated
    public CColor color() {
        return color;
    }

    /**
     * @exclude
     */
    @Deprecated
    public float value() {
        return _myValue;
    }

    /**
     * @exclude
     */
    @Deprecated
    public String stringValue() {
        return getStringValue();
    }

    /**
     * @exclude
     */
    @Deprecated
    public float[] arrayValue() {
        return getArrayValue();
    }

    /**
     * @exclude
     */
    @Deprecated
    public String name() {
        return _myName;
    }

    /**
     * @exclude
     */
    @Deprecated
    public int id() {
        return _myId;
    }

    /**
     * @exclude
     */
    @Deprecated
    public Controller<?> controller(String theController) {
        return cp5.getController(theController);
    }

    /**
     * @exclude
     */
    @Deprecated
    public ControllerInterface<?> parent() {
        return _myParent;
    }

    /**
     * @exclude @deprecated
     */
    @Deprecated
    public Label captionLabel() {
        return _myLabel;
    }

    /**
     * @exclude @deprecated
     */
    @Deprecated
    public Label valueLabel() {
        return _myValueLabel;
    }

}
