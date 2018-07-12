package de.metas.material.dispo.commons.repository;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-material-dispo-commons
 * %%
 * Copyright (C) 2018 metas GmbH
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

@Value
public class DateAndSeqNo
{
	LocalDateTime date;
	int seqNo;

	public boolean isBefore(@NonNull final DateAndSeqNo other)
	{
		final boolean beforeDate = date.isBefore(other.getDate());

		final boolean sameDateDateAndSmallerSeqNo = date.equals(other.getDate()) && seqNo < other.getSeqNo();

		return beforeDate || sameDateDateAndSmallerSeqNo;
	}

	public DateAndSeqNo latest(@Nullable final DateAndSeqNo other)
	{
		if (other == null)
		{
			return this;
		}
		if (other.isBefore(this))
		{
			return this;
		}

		return other;
	}
}