package de.metas.ui.web.order.sales.purchasePlanning.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import de.metas.money.Currency;
import de.metas.printing.esb.base.util.Check;
import de.metas.purchasecandidate.BPPurchaseScheduleService;
import de.metas.purchasecandidate.PurchaseCandidate;
import de.metas.purchasecandidate.PurchaseCandidateRepository;
import de.metas.purchasecandidate.PurchaseDemand;
import de.metas.purchasecandidate.PurchaseDemandId;
import de.metas.purchasecandidate.PurchaseDemandWithCandidates;
import de.metas.purchasecandidate.SalesOrder;
import de.metas.purchasecandidate.SalesOrderLine;
import de.metas.purchasecandidate.SalesOrderLineRepository;
import de.metas.purchasecandidate.SalesOrderLines;
import de.metas.purchasecandidate.availability.AvailabilityException;
import de.metas.purchasecandidate.availability.AvailabilityResult;
import de.metas.purchasecandidate.grossprofit.PurchaseProfitInfoFactory;
import de.metas.quantity.Quantity;
import de.metas.ui.web.view.IView;
import de.metas.ui.web.view.event.ViewChangesCollector;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

@ToString(exclude = { "purchaseRowFactory", "viewSupplier" })
class PurchaseRowsLoader
{
	// services
	private final PurchaseRowFactory purchaseRowFactory;
	private final PurchaseCandidateRepository purchaseCandidateRepository;
	private final BPPurchaseScheduleService bpPurchaseScheduleService;
	// private final IWarehouseDAO warehouseDAO = Services.get(IWarehouseDAO.class);
	// private final IBPartnerProductDAO partnerProductDAO = Services.get(IBPartnerProductDAO.class);
	private final PurchaseProfitInfoFactory purchaseProfitInfoFactory;
	private final SalesOrderLineRepository salesOrderLineRepository;

	// parameters
	private final SalesOrderLines salesOrderLines;
	private final Supplier<IView> viewSupplier;

	private ImmutableMap<PurchaseCandidate, PurchaseRow> purchaseCandidate2purchaseRow;

	@Builder
	private PurchaseRowsLoader(
			@NonNull final SalesOrderLines salesOrderLines,
			@NonNull final Supplier<IView> viewSupplier,
			//
			@NonNull final PurchaseRowFactory purchaseRowFactory,
			@NonNull final PurchaseCandidateRepository purchaseCandidateRepository,
			@NonNull final BPPurchaseScheduleService bpPurchaseScheduleService,
			@NonNull final PurchaseProfitInfoFactory purchaseProfitInfoFactory,
			@NonNull final SalesOrderLineRepository salesOrderLineRepository)
	{
		this.salesOrderLines = salesOrderLines;
		this.viewSupplier = viewSupplier;

		this.purchaseRowFactory = purchaseRowFactory;
		this.purchaseCandidateRepository = purchaseCandidateRepository;
		this.bpPurchaseScheduleService = bpPurchaseScheduleService;
		this.purchaseProfitInfoFactory = purchaseProfitInfoFactory;
		this.salesOrderLineRepository = salesOrderLineRepository;
	}

	public List<PurchaseRow> load()
	{
		final ImmutableList.Builder<PurchaseRow> result = ImmutableList.builder();
		final ImmutableMap.Builder<PurchaseCandidate, PurchaseRow> purchaseCandidate2purchaseRowBuilder = ImmutableMap.builder();

		for (final PurchaseDemandWithCandidates demandWithCandidates : salesOrderLines.getSalesOrderLinesWithCandidates())
		{
			final PurchaseDemand demand = demandWithCandidates.getPurchaseDemand();

			final Currency currencyOfParentRow = demand.getCurrency();

			final ImmutableList.Builder<PurchaseRow> rows = ImmutableList.builder();
			for (final PurchaseCandidate purchaseCandidate : demandWithCandidates.getPurchaseCandidates())
			{
				final PurchaseRow candidateRow = purchaseRowFactory
						.rowFromPurchaseCandidateBuilder()
						.purchaseCandidate(purchaseCandidate)
						.vendorProductInfo(purchaseCandidate.getVendorProductInfo())
						.datePromised(TimeUtil.asLocalDateTime(demand.getDatePromised()))
						.currencyOfParentRow(currencyOfParentRow)
						.build();

				purchaseCandidate2purchaseRowBuilder.put(purchaseCandidate, candidateRow);
				rows.add(candidateRow);
			}

			final PurchaseRow groupRow = purchaseRowFactory.createGroupRow(demand, rows.build());
			result.add(groupRow);

		}
		purchaseCandidate2purchaseRow = purchaseCandidate2purchaseRowBuilder.build();

		return result.build();
	}

	private static PurchaseDemand createDemand(final SalesOrderLine salesOrderLine)
	{
		final SalesOrder salesOrder = salesOrderLine.getOrder();

		final Quantity qtyToPurchase = salesOrderLine.getOrderedQty().subtract(salesOrderLine.getDeliveredQty());

		return PurchaseDemand.builder()
				.id(PurchaseDemandId.ofOrderLineId(salesOrderLine.getId()))
				//
				.orgId(salesOrderLine.getOrgId())
				.warehouseId(salesOrderLine.getWarehouseId())
				//
				.productId(salesOrderLine.getProductId())
				.attributeSetInstanceId(salesOrderLine.getAsiId())
				.qtyToDeliver(qtyToPurchase)
				//
				.currency(salesOrderLine.getPriceActual().getCurrency())
				//
				.datePromised(TimeUtil.asLocalDateTime(salesOrderLine.getDatePromised()))
				.preparationDate(salesOrder.getPreparationDate())
				.build();
	}

	public void createAndAddAvailabilityResultRows()
	{
		Check.assumeNotNull(purchaseCandidate2purchaseRow, "purchaseCandidate2purchaseRow was already loaded via load(); this={}", this);

		try
		{
			final Multimap<PurchaseCandidate, AvailabilityResult> availabilityCheckResult;
			availabilityCheckResult = salesOrderLines.checkAvailability();

			handleResultForAsyncAvailabilityCheck(availabilityCheckResult);
		}
		catch (final Throwable throwable)
		{
			handleThrowableForAsyncAvailabilityCheck(throwable);
		}
	}

	public void createAndAddAvailabilityResultRowsAsync()
	{
		Check.assumeNotNull(purchaseCandidate2purchaseRow, "purchaseCandidate2purchaseRow was already loaded via load(); this={}", this);

		salesOrderLines.checkAvailabilityAsync((availabilityCheckResult, throwable) -> {

			handleResultForAsyncAvailabilityCheck(availabilityCheckResult);
			handleThrowableForAsyncAvailabilityCheck(Util.coalesce(throwable.getCause(), throwable));
		});
	}

	private void handleResultForAsyncAvailabilityCheck(
			@Nullable final Multimap<PurchaseCandidate, AvailabilityResult> availabilityCheckResult)
	{
		if (availabilityCheckResult == null)
		{
			return;
		}
		final Set<Entry<PurchaseCandidate, Collection<AvailabilityResult>>> entrySet = //
				availabilityCheckResult.asMap().entrySet();

		final List<DocumentId> changedRowIds = new ArrayList<>();

		for (final Entry<PurchaseCandidate, Collection<AvailabilityResult>> entry : entrySet)
		{
			final PurchaseRow purchaseRowToAugment = purchaseCandidate2purchaseRow.get(entry.getKey());
			final ImmutableList.Builder<PurchaseRow> availabilityResultRows = ImmutableList.builder();

			for (final AvailabilityResult availabilityResult : entry.getValue())
			{
				final PurchaseRow availabilityResultRow = purchaseRowFactory.rowFromAvailabilityResultBuilder()
						.parentRow(purchaseRowToAugment)
						.availabilityResult(availabilityResult).build();

				availabilityResultRows.add(availabilityResultRow);
			}
			purchaseRowToAugment.setAvailabilityInfoRows(availabilityResultRows.build());
			changedRowIds.add(purchaseRowToAugment.getId());
		}

		notifyViewOfChanges(changedRowIds);
	}

	private void handleThrowableForAsyncAvailabilityCheck(@Nullable final Throwable throwable)
	{
		if (throwable == null)
		{
			return;
		}
		if (throwable instanceof AvailabilityException)
		{
			final AvailabilityException availabilityException = (AvailabilityException)throwable;

			final List<DocumentId> changedRowIds = new ArrayList<>();

			final Set<Entry<PurchaseCandidate, Throwable>> entrySet = availabilityException.getPurchaseCandidate2Throwable().entrySet();
			for (final Entry<PurchaseCandidate, Throwable> purchaseCandidate2throwable : entrySet)
			{
				final PurchaseRow purchaseRowToAugment = purchaseCandidate2purchaseRow.get(purchaseCandidate2throwable.getKey());

				final PurchaseRow availabilityResultRow = purchaseRowFactory
						.rowFromThrowableBuilder()
						.parentRow(purchaseRowToAugment)
						.throwable(purchaseCandidate2throwable.getValue())
						.build();

				purchaseRowToAugment.setAvailabilityInfoRows(ImmutableList.of(availabilityResultRow));
				changedRowIds.add(purchaseRowToAugment.getId());
			}

			notifyViewOfChanges(changedRowIds);
		}
		else
		{
			// TODO: display an error-message in the webui
		}
	}

	private void notifyViewOfChanges(final List<DocumentId> changedRowIds)
	{
		final IView view = viewSupplier.get();
		if (view != null)
		{
			ViewChangesCollector
					.getCurrentOrAutoflush()
					.collectRowsChanged(view, DocumentIdsSelection.of(changedRowIds));
		}
	}
}
