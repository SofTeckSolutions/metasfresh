package de.metas.paypalplus.controller;

import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;

import java.time.LocalDate;
import java.util.Optional;

import org.adempiere.service.ClientId;
import org.adempiere.service.OrgId;
import org.adempiere.test.AdempiereTestHelper;
import org.compiere.model.I_C_Currency;

import com.google.common.collect.ImmutableList;

import de.metas.bpartner.BPartnerContactId;
import de.metas.email.EMailAddress;
import de.metas.email.MailService;
import de.metas.email.mailboxes.MailboxRepository;
import de.metas.email.templates.MailTemplateRepository;
import de.metas.money.CurrencyId;
import de.metas.money.CurrencyRepository;
import de.metas.money.Money;
import de.metas.order.OrderId;
import de.metas.payment.PaymentRule;
import de.metas.payment.processor.PaymentProcessorService;
import de.metas.payment.reservation.PaymentReservationCreateRequest;
import de.metas.payment.reservation.PaymentReservationRepository;
import de.metas.payment.reservation.PaymentReservationService;
import de.metas.paypalplus.logs.PayPalLogRepository;
import de.metas.paypalplus.orders.PayPalOrderRepository;
import de.metas.paypalplus.orders.PayPalOrderService;
import de.metas.paypalplus.processor.PayPalPaymentProcessor;

/*
 * #%L
 * de.metas.payment.paypalplus
 * %%
 * Copyright (C) 2019 metas GmbH
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

public class PayPalCheckoutManualTest2
{
	public static void main(final String[] args) throws Exception
	{
		AdempiereTestHelper.get().init();

		new PayPalCheckoutManualTest2().run();
		System.out.println("-------------------------------------------------------------------------");
		System.out.println("Done");
	}

	private ClientId clientId = ClientId.ofRepoId(1);
	private final OrgId orgId = OrgId.ofRepoId(1);
	private final CurrencyId currencyId;

	private final PaymentReservationService paymentReservationService;

	private PayPalCheckoutManualTest2()
	{
		paymentReservationService = createPaymentReservationService();

		//
		currencyId = createCurrency("EUR");
	}

	private static PaymentReservationService createPaymentReservationService()
	{
		final PayPalOrderRepository payPalOrderRepository = new PayPalOrderRepository(Optional.empty());
		final PayPalOrderService payPalOrdersService = new PayPalOrderService(payPalOrderRepository);

		final MailService mailService = new MailService(new MailboxRepository(), new MailTemplateRepository());
		
		final PayPalPaymentProcessor payPalProcessor = new PayPalPaymentProcessor(
				new TestPayPalConfigProvider(),
				payPalOrdersService,
				new PayPalLogRepository(Optional.empty()),
				new CurrencyRepository(),
				mailService);

		final PaymentProcessorService paymentProcessors = new PaymentProcessorService(Optional.of(ImmutableList.of(payPalProcessor)));

		return new PaymentReservationService(
				new PaymentReservationRepository(),
				paymentProcessors);
	}

	private static CurrencyId createCurrency(String currencyCode)
	{
		final I_C_Currency currency = newInstance(I_C_Currency.class);
		currency.setISO_Code(currencyCode);
		currency.setStdPrecision(2);
		saveRecord(currency);
		return CurrencyId.ofRepoId(currency.getC_Currency_ID());
	}

	private void run()
	{
		final OrderId salesOrderId = OrderId.ofRepoId(123);

		paymentReservationService.create(PaymentReservationCreateRequest.builder()
				.clientId(clientId)
				.orgId(orgId)
				.amount(Money.of(100, currencyId))
				.payerContactId(BPartnerContactId.ofRepoId(1, 2))
				.payerEmail(EMailAddress.ofNullableString("from@example.com"))
				.salesOrderId(salesOrderId)
				.dateTrx(LocalDate.now())
				.paymentRule(PaymentRule.PayPal)
				.build());
	}

}
