package org.adempiere.invoice.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.util.Arrays;
import java.util.List;

import org.adempiere.service.ClientId;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.test.AdempiereTestHelper;
import org.compiere.model.I_M_InOutLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.metas.adempiere.model.I_C_InvoiceLine;
import de.metas.organization.OrgId;
import de.metas.util.Services;

public class InvoiceBLSortLinesTests
{
	I_C_InvoiceLine il1;
	I_C_InvoiceLine il2;
	I_C_InvoiceLine il3;
	I_C_InvoiceLine il4;
	I_C_InvoiceLine il5;

	I_M_InOutLine iol1;
	I_M_InOutLine iol2;
	I_M_InOutLine iol3;
	I_M_InOutLine iol4;
	I_M_InOutLine iol5;

	@BeforeEach
	public final void initStuff()
	{
		AdempiereTestHelper.get().init();
		Services.get(ISysConfigBL.class).setValue(AbstractInvoiceBL.SYSCONFIG_SortILsByShipmentLineOrders, false, ClientId.SYSTEM, OrgId.ANY);

		il1 = Mockito.mock(I_C_InvoiceLine.class);
		il2 = Mockito.mock(I_C_InvoiceLine.class);
		il3 = Mockito.mock(I_C_InvoiceLine.class);
		il4 = Mockito.mock(I_C_InvoiceLine.class);
		il5 = Mockito.mock(I_C_InvoiceLine.class);

		iol1 = Mockito.mock(I_M_InOutLine.class);
		iol2 = Mockito.mock(I_M_InOutLine.class);
		iol3 = Mockito.mock(I_M_InOutLine.class);
		iol4 = Mockito.mock(I_M_InOutLine.class);
		iol5 = Mockito.mock(I_M_InOutLine.class);
	}

	/**
	 * Test Case 1: no changes, when already sorted.
	 */
	@Test
	public void sortDontChange()
	{
		hasIDs(il1, iol1, 1, 0, false, 10);
		hasIDs(il2, iol2, 2, 11, false, 20);
		hasIDs(il3, iol3, 3, 11, true, 30);
		hasIDs(il4, iol4, 4, 12, false, 40);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il1);
		assertSame(sorted.get(1), il2);
		assertSame(sorted.get(2), il3);
		assertSame(sorted.get(3), il4);
	}

	/**
	 * Test Case 2: Sort InOut_IDs
	 */
	@Test
	public void sortInOuts()
	{
		hasIDs(il1, iol1, 1, 12, false, 10);
		hasIDs(il2, iol2, 2, 11, false, 10);
		hasIDs(il3, iol3, 3, 12, false, 10);
		hasIDs(il4, iol4, 4, 10, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il4); // inOut 10
		assertSame(sorted.get(1), il2); // inOut 11
		assertSame(sorted.get(2), il1); // inOut 12
		assertSame(sorted.get(3), il3); // inOut 12
		// the Last two rows are in the same order they were before
	}

	/**
	 * Test Case 3: Seek InOut_ID for Lines Without InOut_ID
	 */
	@Test
	public void sortSeekInOut()
	{
		hasIDs(il1, iol1, 1, 0, false, 8);
		hasIDs(il2, iol2, 2, 0, false, 9);
		hasIDs(il3, iol3, 3, 12, false, 10);
		hasIDs(il4, iol4, 4, 10, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il4); // inOut 10
		assertSame(sorted.get(1), il1); // inOut 12
		assertSame(sorted.get(2), il2); // inOut 12
		assertSame(sorted.get(3), il3); // inOut 12
		// the Last three rows are also ordered by lineNo
	}

	/**
	 * Test Case 4: Sort Freight Cost Lines
	 */
	@Test
	public void sortFreightCost()
	{
		hasIDs(il1, iol1, 1, 11, true, 10);
		hasIDs(il2, iol2, 2, 11, false, 10);
		hasIDs(il3, iol3, 3, 11, true, 10);
		hasIDs(il4, iol4, 4, 11, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il2);
		assertSame(sorted.get(1), il4);
		assertSame(sorted.get(2), il1);
		assertSame(sorted.get(3), il3);
	}

	/**
	 * Test Case 5: Sort Lines after Line number
	 */
	@Test
	public void sortLineNo()
	{
		hasIDs(il1, iol1, 1, 11, false, 20);
		hasIDs(il2, iol2, 2, 11, false, 10);
		hasIDs(il3, iol3, 3, 11, false, 20);
		hasIDs(il4, iol4, 4, 11, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il2);
		assertSame(sorted.get(1), il4);
		assertSame(sorted.get(2), il1);
		assertSame(sorted.get(3), il3);
	}

	/**
	 * Test Case 6: Sort by all criteria
	 */
	@Test
	public void sortComplete()
	{
		hasIDs(il1, iol1, 1, 12, false, 10);
		hasIDs(il2, iol2, 2, 0, false, 9);
		hasIDs(il3, iol3, 3, 0, false, 8);
		hasIDs(il4, iol4, 4, 11, true, 10);
		hasIDs(il5, iol5, 5, 11, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4, il5 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il3);
		assertSame(sorted.get(1), il2);
		assertSame(sorted.get(2), il5);
		assertSame(sorted.get(3), il4);
		assertSame(sorted.get(4), il1);
	}

	/**
	 * Test Case 7 (08295): Sort with override for order/line order first
	 */
	@Test
	public void sortWith_SYSCONFIG_SortILsByShipmentLineOrders()
	{
		Services.get(ISysConfigBL.class).setValue(AbstractInvoiceBL.SYSCONFIG_SortILsByShipmentLineOrders, true, ClientId.METASFRESH, OrgId.ANY); // configure override

		hasIDs(il1, iol1, 1, 12, false, 10);
		hasIDs(il2, iol2, 2, 0, false, 9);
		hasIDs(il3, iol3, 3, 0, false, 8);
		hasIDs(il4, iol4, 4, 11, true, 10);
		hasIDs(il5, iol5, 5, 11, false, 10);

		final List<I_C_InvoiceLine> sorted = Arrays.asList(new I_C_InvoiceLine[] { il1, il2, il3, il4, il5 });
		new InvoiceBL().sortLines(sorted);

		assertSame(sorted.get(0), il5);
		assertSame(sorted.get(1), il4);
		assertSame(sorted.get(2), il1);
		assertSame(sorted.get(3), il3);
		assertSame(sorted.get(4), il2);
	}

	public void hasIDs(
			final I_C_InvoiceLine il,
			final I_M_InOutLine iol,
			final int invoiceLineId,
			final int inoutId,
			final boolean freightCost,
			final int lineNo)
	{
		if (inoutId <= 0)
		{
			Mockito.when(il.getC_InvoiceLine_ID()).thenReturn(invoiceLineId);
			Mockito.when(il.getM_InOutLine()).thenReturn(null);
			Mockito.when(il.getM_InOutLine_ID()).thenReturn(0);
			Mockito.when(il.isFreightCostLine()).thenReturn(freightCost);
			Mockito.when(il.getLine()).thenReturn(lineNo);
		}
		else
		{
			Mockito.when(il.getC_InvoiceLine_ID()).thenReturn(invoiceLineId);
			Mockito.when(il.getM_InOutLine_ID()).thenReturn(1);
			Mockito.when(il.getM_InOutLine()).thenReturn(iol);
			Mockito.when(il.isFreightCostLine()).thenReturn(freightCost);
			Mockito.when(il.getLine()).thenReturn(lineNo);
			
			Mockito.when(iol.getM_InOut_ID()).thenReturn(inoutId);
		}

	}
}
