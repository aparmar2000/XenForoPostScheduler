package aparmar2000.xenforoposter.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.settings.defs.BooleanSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.ChoiceSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.IntegerSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.MultilineStringSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.StringSettingDefinition;

class SettingsDefinitionTest {

	@Test
	@DisplayName("BooleanSettingDefinition should support UI controls, value binding, serialization, and listeners")
	void testBooleanSettingDefinition() {
		BooleanSettingDefinition boolDef = new BooleanSettingDefinition("test_bool", "Bool Setting", "Desc", true, "Options");

		assertEquals("test_bool", boolDef.getKey());
		assertEquals("Bool Setting", boolDef.getLabel());
		assertEquals("Desc", boolDef.getDescription());
		assertEquals("Options", boolDef.getGroup());
		assertEquals(Boolean.class, boolDef.getValueType());
		assertTrue(boolDef.getDefaultValue());
		assertTrue(boolDef.getUiValue());
		assertNotNull(boolDef.getUiComponent());
		assertTrue(boolDef.getUiComponent() instanceof JCheckBox);
		assertFalse(boolDef.expandsHorizontally());
		assertFalse(boolDef.expandsVertically());

		// Validation
		assertTrue(boolDef.validate(true));
		assertTrue(boolDef.validate(false));
		assertFalse(boolDef.validate(null));
		assertTrue(boolDef.isUiValueValid());

		// Serialization and Deserialization
		assertEquals("true", boolDef.serialize(true));
		assertEquals("false", boolDef.serialize(false));
		assertTrue(boolDef.deserialize("true"));
		assertFalse(boolDef.deserialize("false"));
		assertFalse(boolDef.deserialize("invalid"));

		// Listener notification on click
		Runnable mockListener = mock(Runnable.class);
		boolDef.addChangeListener(mockListener);
		((JCheckBox) boolDef.getUiComponent()).doClick();
		assertFalse(boolDef.getUiValue());
		verify(mockListener, atLeastOnce()).run();

		// UI value setter & reset
		boolDef.setUiValue(false);
		assertFalse(boolDef.getUiValue());
		boolDef.resetToDefault();
		assertTrue(boolDef.getUiValue());

		// Constructor without group
		BooleanSettingDefinition noGroupDef = new BooleanSettingDefinition("bool_nogroup", "Label", null, false);
		assertNull(noGroupDef.getGroup());
		assertEquals("", noGroupDef.getDescription());
		assertFalse(noGroupDef.getDefaultValue());
	}

	@Test
	@DisplayName("IntegerSettingDefinition should enforce ranges, clamp UI inputs, and handle parsing")
	void testIntegerSettingDefinition() {
		IntegerSettingDefinition intDef =
				new IntegerSettingDefinition("test_int", "Int Setting", "Desc", 10, 0, 100, "Limits");

		assertEquals("test_int", intDef.getKey());
		assertEquals("Int Setting", intDef.getLabel());
		assertEquals("Desc", intDef.getDescription());
		assertEquals("Limits", intDef.getGroup());
		assertEquals(Integer.class, intDef.getValueType());
		assertEquals(0, intDef.getMinValue());
		assertEquals(100, intDef.getMaxValue());
		assertEquals(10, intDef.getDefaultValue());
		assertEquals(10, intDef.getUiValue());
		assertTrue(intDef.getUiComponent() instanceof JSpinner);
		assertFalse(intDef.expandsHorizontally());
		assertFalse(intDef.expandsVertically());

		// Validation
		assertTrue(intDef.validate(0));
		assertTrue(intDef.validate(50));
		assertTrue(intDef.validate(100));
		assertFalse(intDef.validate(-1));
		assertFalse(intDef.validate(101));
		assertFalse(intDef.validate(null));
		assertTrue(intDef.isUiValueValid());

		// Clamping on setUiValue
		intDef.setUiValue(25);
		assertEquals(25, intDef.getUiValue());
		intDef.setUiValue(500); // Clamped to max
		assertEquals(100, intDef.getUiValue());
		intDef.setUiValue(-50); // Clamped to min
		assertEquals(0, intDef.getUiValue());

		// Reset
		intDef.resetToDefault();
		assertEquals(10, intDef.getUiValue());

		// Serialization and Deserialization
		assertEquals("25", intDef.serialize(25));
		assertEquals(50, intDef.deserialize("50"));
		assertEquals(100, intDef.deserialize("999")); // Clamped to max
		assertEquals(0, intDef.deserialize("-999")); // Clamped to min
		assertEquals(10, intDef.deserialize("not_a_number")); // Defaults

		// Listener notification on spinner change
		Runnable mockListener = mock(Runnable.class);
		intDef.addChangeListener(mockListener);
		intDef.getSpinner().setValue(30);
		verify(mockListener, atLeastOnce()).run();

		// Constructor without group
		IntegerSettingDefinition noGroupDef = new IntegerSettingDefinition("int_nogroup", "Label", null, 5, 1, 10);
		assertNull(noGroupDef.getGroup());
		assertEquals("", noGroupDef.getDescription());
	}

	@Test
	@DisplayName("ChoiceSettingDefinition should constrain values to options and bind to combo box")
	void testChoiceSettingDefinition() {
		List<String> options = List.of("OptA", "OptB", "OptC");
		ChoiceSettingDefinition choiceDef =
				new ChoiceSettingDefinition("test_choice", "Choice", "Desc", "OptA", options, "Options");

		assertEquals("test_choice", choiceDef.getKey());
		assertEquals("Choice", choiceDef.getLabel());
		assertEquals("Desc", choiceDef.getDescription());
		assertEquals("Options", choiceDef.getGroup());
		assertEquals(String.class, choiceDef.getValueType());
		assertEquals("OptA", choiceDef.getDefaultValue());
		assertEquals("OptA", choiceDef.getUiValue());
		assertEquals(options, choiceDef.getOptions());
		assertTrue(choiceDef.getUiComponent() instanceof JComboBox);
		assertFalse(choiceDef.expandsHorizontally());
		assertFalse(choiceDef.expandsVertically());

		// Validation
		assertTrue(choiceDef.validate("OptA"));
		assertTrue(choiceDef.validate("OptB"));
		assertFalse(choiceDef.validate("InvalidOpt"));
		assertFalse(choiceDef.validate(null));
		assertTrue(choiceDef.isUiValueValid());

		// Serialization and Deserialization
		assertEquals("OptB", choiceDef.serialize("OptB"));
		assertEquals("OptA", choiceDef.serialize(null));
		assertEquals("OptC", choiceDef.deserialize("OptC"));
		assertEquals("OptA", choiceDef.deserialize("UnknownOpt"));
		assertEquals("OptA", choiceDef.deserialize(null));

		// Value mutation via setter
		choiceDef.setUiValue("OptB");
		assertEquals("OptB", choiceDef.getUiValue());
		choiceDef.setUiValue("InvalidOpt"); // Should ignore invalid option
		assertEquals("OptB", choiceDef.getUiValue());

		// Reset
		choiceDef.resetToDefault();
		assertEquals("OptA", choiceDef.getUiValue());

		// Listener notification on selection change
		Runnable mockListener = mock(Runnable.class);
		choiceDef.addChangeListener(mockListener);
		choiceDef.getComboBox().setSelectedItem("OptC");
		verify(mockListener, atLeastOnce()).run();

		// Constructor without group
		ChoiceSettingDefinition noGroupDef = new ChoiceSettingDefinition("choice_nogroup", "Label", null, "OptA", options);
		assertNull(noGroupDef.getGroup());
		assertEquals("", noGroupDef.getDescription());
	}

	@Test
	@DisplayName("StringSettingDefinition should manage single-line text input, expansion, and document events")
	void testStringSettingDefinition() {
		StringSettingDefinition strDef = new StringSettingDefinition("test_str", "Str", "Desc", "hello", "General");

		assertEquals("test_str", strDef.getKey());
		assertEquals("Str", strDef.getLabel());
		assertEquals("Desc", strDef.getDescription());
		assertEquals("General", strDef.getGroup());
		assertEquals(String.class, strDef.getValueType());
		assertEquals("hello", strDef.getDefaultValue());
		assertEquals("hello", strDef.getUiValue());
		assertTrue(strDef.getUiComponent() instanceof JTextField);
		assertTrue(strDef.expandsHorizontally());
		assertFalse(strDef.expandsVertically());

		// Validation
		assertTrue(strDef.validate("any string"));
		assertTrue(strDef.validate(""));
		assertFalse(strDef.validate(null));
		assertTrue(strDef.isUiValueValid());

		// Serialization and Deserialization
		assertEquals("text", strDef.serialize("text"));
		assertEquals("", strDef.serialize(null));
		assertEquals("parsed", strDef.deserialize("parsed"));
		assertEquals("hello", strDef.deserialize(null));

		// Set value and reset
		strDef.setUiValue("world");
		assertEquals("world", strDef.getUiValue());
		strDef.setUiValue(null);
		assertEquals("", strDef.getUiValue());
		strDef.resetToDefault();
		assertEquals("hello", strDef.getUiValue());

		// Listener notifications on text change and focus lost
		Runnable mockListener = mock(Runnable.class);
		strDef.addChangeListener(mockListener);
		JTextField tf = (JTextField) strDef.getUiComponent();
		tf.setText("changed");
		verify(mockListener, atLeastOnce()).run();

		for (FocusListener fl : tf.getFocusListeners()) {
			fl.focusLost(new FocusEvent(tf, FocusEvent.FOCUS_LOST));
		}
		verify(mockListener, atLeast(2)).run();

		// Constructor without group
		StringSettingDefinition noGroupDef = new StringSettingDefinition("str_nogroup", "Label", null, "default");
		assertNull(noGroupDef.getGroup());
		assertEquals("", noGroupDef.getDescription());
	}

	@Test
	@DisplayName("MultilineStringSettingDefinition should manage multi-line text input with scroll panes and 2D expansion")
	void testMultilineStringSettingDefinition() {
		MultilineStringSettingDefinition multiDef =
				new MultilineStringSettingDefinition("test_multi", "Multi", "Desc", "Line1\nLine2", "General");

		assertEquals("test_multi", multiDef.getKey());
		assertEquals("Multi", multiDef.getLabel());
		assertEquals("Desc", multiDef.getDescription());
		assertEquals("General", multiDef.getGroup());
		assertEquals(String.class, multiDef.getValueType());
		assertEquals("Line1\nLine2", multiDef.getDefaultValue());
		assertEquals("Line1\nLine2", multiDef.getUiValue());
		assertTrue(multiDef.getUiComponent() instanceof JScrollPane);
		assertTrue(multiDef.expandsHorizontally());
		assertTrue(multiDef.expandsVertically());

		// Validation
		assertTrue(multiDef.validate("multi\nline"));
		assertFalse(multiDef.validate(null));
		assertTrue(multiDef.isUiValueValid());

		// Serialization and Deserialization
		assertEquals("multi\nline", multiDef.serialize("multi\nline"));
		assertEquals("", multiDef.serialize(null));
		assertEquals("multi\nline", multiDef.deserialize("multi\nline"));
		assertEquals("Line1\nLine2", multiDef.deserialize(null));

		// Set value and reset
		multiDef.setUiValue("Updated");
		assertEquals("Updated", multiDef.getUiValue());
		multiDef.setUiValue(null);
		assertEquals("", multiDef.getUiValue());
		multiDef.resetToDefault();
		assertEquals("Line1\nLine2", multiDef.getUiValue());

		// Constructor with custom rows and columns
		MultilineStringSettingDefinition customRowsColsDef =
				new MultilineStringSettingDefinition("custom_multi", "Label", null, "def", 5, 40, "CustomGroup");
		assertEquals("CustomGroup", customRowsColsDef.getGroup());
		assertEquals("", customRowsColsDef.getDescription());
		assertEquals("def", customRowsColsDef.getUiValue());

		// Constructor without group
		MultilineStringSettingDefinition noGroupDef =
				new MultilineStringSettingDefinition("nogroup_multi", "Label", "Desc", "def");
		assertNull(noGroupDef.getGroup());
	}

	@Test
	@DisplayName("SettingDefinition base class should manage change listeners and trim group strings")
	void testBaseSettingDefinitionBehavior() {
		StringSettingDefinition trimmedGroupDef =
				new StringSettingDefinition("key", "Label", null, "val", "   TrimmedGroup   ");
		assertEquals("TrimmedGroup", trimmedGroupDef.getGroup());

		StringSettingDefinition emptyGroupDef =
				new StringSettingDefinition("key", "Label", null, "val", "     ");
		assertNull(emptyGroupDef.getGroup());

		// Listener registration, unregistration, clearing, and error resilience
		Runnable goodListener = mock(Runnable.class);
		Runnable failingListener = () -> {
			throw new RuntimeException("Listener boom");
		};

		trimmedGroupDef.addChangeListener(failingListener);
		trimmedGroupDef.addChangeListener(goodListener);

		trimmedGroupDef.setUiValue("trigger");
		// Failing listener should not prevent goodListener from running
		verify(goodListener, atLeastOnce()).run();

		trimmedGroupDef.removeChangeListener(goodListener);
		trimmedGroupDef.clearChangeListeners();
		assertTrue(trimmedGroupDef.getChangeListeners().isEmpty());
	}
}

