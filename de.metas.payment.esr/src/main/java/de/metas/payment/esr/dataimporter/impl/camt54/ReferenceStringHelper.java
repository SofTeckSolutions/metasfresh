package de.metas.payment.esr.dataimporter.impl.camt54;

import java.util.Optional;

import org.compiere.util.Env;

import com.google.common.annotations.VisibleForTesting;

import de.metas.i18n.IMsgBL;
import de.metas.payment.camt054_001_02.EntryTransaction2;
import de.metas.payment.camt054_001_06.EntryTransaction8;
import de.metas.payment.esr.dataimporter.ESRTransaction.ESRTransactionBuilder;
import de.metas.util.Services;

import lombok.NonNull;

/*
 * #%L
 * de.metas.payment.esr
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

/**
 * This little helper class has the job of getting the (ESR) reference and passing it to an {@link ESRTransactionBuilder}.
 * There is also some fallback and error messages to be done in case of some not-so-happy scenarios.
 * 
 * Note: codebeat keeps complaining about how spagetti {@link ESRDataImporterCamt54} is, so from time to time I extract something :-D.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class ReferenceStringHelper
{

	@VisibleForTesting
	static final String MSG_AMBIGOUS_REFERENCE = "ESR_CAMT54_Ambigous_Reference";

	@VisibleForTesting
	static final String MSG_MISSING_ESR_REFERENCE = "ESR_CAMT54_Missing_ESR_Reference";

	/**
	 * This constant is used as value in {@code /BkToCstmrDbtCdtNtfctn/Ntfctn/Ntry/NtryDtls/TxDtls/RmtInf/Strd/CdtrRefInf/Tp/CdOrPrtry/Prtry} to indicate that the references given in {@code CdtrRefInf} is an ESR number.
	 */
	private static final String ISR_REFERENCE = "ISR Reference";

	/**
	 * extractAndSetEsrReference for version 6 <code>BankToCustomerDebitCreditNotificationV06</code>
	 * @param txDtls
	 * @param trxBuilder
	 */
	public void extractAndSetEsrReference(
			@NonNull final EntryTransaction8 txDtls,
			@NonNull final ESRTransactionBuilder trxBuilder)
	{
		final IMsgBL msgBL = Services.get(IMsgBL.class);

		final Optional<String> esrReferenceNumberString = extractEsrReference(txDtls);
		if (esrReferenceNumberString.isPresent())
		{
			trxBuilder.esrReferenceNumber(esrReferenceNumberString.get());
		}
		else
		{
			final Optional<String> fallback = extractReferenceFallback(txDtls);
			if (fallback.isPresent())
			{
				trxBuilder.esrReferenceNumber(fallback.get());
				trxBuilder.errorMsg(msgBL.getMsg(Env.getCtx(), MSG_AMBIGOUS_REFERENCE));
			}
			else
			{
				trxBuilder.errorMsg(msgBL.getMsg(Env.getCtx(), MSG_MISSING_ESR_REFERENCE));
			}
		}
	}
	
	/**
	 * extractAndSetEsrReference for version 2 <code>BankToCustomerDebitCreditNotificationV02</code>
	 * @param txDtls
	 * @param trxBuilder
	 */
	public void extractAndSetEsrReference(
			@NonNull final EntryTransaction2 txDtls,
			@NonNull final ESRTransactionBuilder trxBuilder)
	{
		final IMsgBL msgBL = Services.get(IMsgBL.class);

		final Optional<String> esrReferenceNumberString = extractEsrReference(txDtls);
		if (esrReferenceNumberString.isPresent())
		{
			trxBuilder.esrReferenceNumber(esrReferenceNumberString.get());
		}
		else
		{
			final Optional<String> fallback = extractReferenceFallback(txDtls);
			if (fallback.isPresent())
			{
				trxBuilder.esrReferenceNumber(fallback.get());
				trxBuilder.errorMsg(msgBL.getMsg(Env.getCtx(), MSG_AMBIGOUS_REFERENCE));
			}
			else
			{
				trxBuilder.errorMsg(msgBL.getMsg(Env.getCtx(), MSG_MISSING_ESR_REFERENCE));
			}
		}
	}

	/**
	 * Gets <code>TxDtls/RmtInf/Strd/CdtrRefInf/Ref</code><br>
	 * from a <code>CdtrRefInf</code> element<br>
	 * that has <code>CdtrRefInf/Tp/CdOrPrtry == "ISR Reference"</code>.
	 * extractEsrReference for version 6 <code>BankToCustomerDebitCreditNotificationV06</code> 
	 * 
	 * @param txDtls
	 * @return
	 * 
	 * @task https://github.com/metasfresh/metasfresh/issues/2107
	 */
	private Optional<String> extractEsrReference(@NonNull final EntryTransaction8 txDtls)
	{
		// get the esr reference string out of the XML tree
		final Optional<String> esrReferenceNumberString = txDtls.getRmtInf().getStrd().stream()
				.map(strd -> strd.getCdtrRefInf())

				// it's stored in the cdtrRefInf records whose cdtrRefInf/tp/cdOrPrtry/prtr equals to ISR_REFERENCE
				.filter(cdtrRefInf -> cdtrRefInf != null
						&& cdtrRefInf.getTp() != null
						&& cdtrRefInf.getTp().getCdOrPrtry() != null
						&& cdtrRefInf.getTp().getCdOrPrtry().getPrtry().equals(ISR_REFERENCE))

				.map(cdtrRefInf -> cdtrRefInf.getRef())
				.findFirst();
		return esrReferenceNumberString;
	}
	
	
	/**
	 * Gets <code>TxDtls/RmtInf/Strd/CdtrRefInf/Ref</code><br>
	 * from a <code>CdtrRefInf</code> element<br>
	 * that has <code>CdtrRefInf/Tp/CdOrPrtry == "ISR Reference"</code>.
	 * extractEsrReference for version 2 <code>BankToCustomerDebitCreditNotificationV02</code>
	 * 
	 * @param txDtls
	 * @return
	 * 
	 * @task https://github.com/metasfresh/metasfresh/issues/2107
	 */
	private Optional<String> extractEsrReference(@NonNull final EntryTransaction2 txDtls)
	{
		// get the esr reference string out of the XML tree
		final Optional<String> esrReferenceNumberString = txDtls.getRmtInf().getStrd().stream()
				.map(strd -> strd.getCdtrRefInf())

				// it's stored in the cdtrRefInf records whose cdtrRefInf/tp/cdOrPrtry/prtr equals to ISR_REFERENCE
				.filter(cdtrRefInf -> cdtrRefInf != null
						&& cdtrRefInf.getTp() != null
						&& cdtrRefInf.getTp().getCdOrPrtry() != null
						&& cdtrRefInf.getTp().getCdOrPrtry().getPrtry().equals(ISR_REFERENCE))

				.map(cdtrRefInf -> cdtrRefInf.getRef())
				.findFirst();
		return esrReferenceNumberString;
	}
	

	/**
	 * extractReferenceFallback for version 6 <code>BankToCustomerDebitCreditNotificationV06</code>
	 * 
	 * @param txDtls
	 * @return
	 * 
	 * @task https://github.com/metasfresh/metasfresh/issues/2107
	 */
	private Optional<String> extractReferenceFallback(@NonNull final EntryTransaction8 txDtls)
	{
		// get the esr reference string out of the XML tree
		final Optional<String> esrReferenceNumberString = txDtls.getRmtInf().getStrd().stream()
				.map(strd -> strd.getCdtrRefInf())
				.filter(cdtrRefInf -> cdtrRefInf != null)
				.map(cdtrRefInf -> cdtrRefInf.getRef())
				.findFirst();
		return esrReferenceNumberString;
	}
	

	/**
	 * extractReferenceFallback for version 2 <code>BankToCustomerDebitCreditNotificationV02</code>
	 * 
	 * @param txDtls
	 * @return
	 * 
	 * @task https://github.com/metasfresh/metasfresh/issues/2107
	 */
	private Optional<String> extractReferenceFallback(@NonNull final EntryTransaction2 txDtls)
	{
		// get the esr reference string out of the XML tree
		final Optional<String> esrReferenceNumberString = txDtls.getRmtInf().getStrd().stream()
				.map(strd -> strd.getCdtrRefInf())
				.filter(cdtrRefInf -> cdtrRefInf != null)
				.map(cdtrRefInf -> cdtrRefInf.getRef())
				.findFirst();
		return esrReferenceNumberString;
	}
}