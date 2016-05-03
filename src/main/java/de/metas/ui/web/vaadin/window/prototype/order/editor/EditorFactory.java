package de.metas.ui.web.vaadin.window.prototype.order.editor;

import de.metas.ui.web.vaadin.window.prototype.order.PropertyDescriptor;
import de.metas.ui.web.vaadin.window.prototype.order.PropertyDescriptorType;
import de.metas.ui.web.vaadin.window.prototype.order.WindowConstants;

/*
 * #%L
 * de.metas.ui.web.vaadin
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class EditorFactory
{
	public Editor createEditor(final PropertyDescriptor descriptor)
	{
		if (descriptor.getType() == PropertyDescriptorType.Tabular)
		{
			return new GridEditor(descriptor);
		}
		else if (descriptor.isValueProperty())
		{
			return createValueEditor(descriptor);
		}
		else if (WindowConstants.PROPERTYNAME_WindowRoot.equals(descriptor.getPropertyName()))
		{
			return new WindowContentRootEditorsContainer(descriptor.getPropertyName());
		}
		else
		{
			return createEditorContainer(descriptor);
		}
	}

	private EditorsContainer createEditorContainer(final PropertyDescriptor descriptor)
	{
		return new FieldEditorsContainer(descriptor);
	}

	private Editor createValueEditor(final PropertyDescriptor descriptor)
	{
		final Class<?> valueType = descriptor.getValueType();

		if (String.class.equals(valueType))
		{
			return new TextEditor(descriptor);
		}
		else if (java.util.Date.class.equals(valueType))
		{
			return new DateEditor(descriptor);
		}
		else if (java.math.BigDecimal.class.equals(valueType))
		{
			return new BigDecimalEditor(descriptor);
		}
		else if (Integer.class.equals(valueType))
		{
			return new IntegerEditor(descriptor);
		}
		else if (LookupValue.class.isAssignableFrom(valueType))
		{
//			return new ComboLookupValueEditor(descriptor);
			return new SearchLookupValueEditor(descriptor);
		}
		else if (ComposedValue.class.isAssignableFrom(valueType))
		{
			return new ComposedValueEditor(descriptor);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported property for " + valueType + " (descriptor: " + descriptor);
		}
	}
}
