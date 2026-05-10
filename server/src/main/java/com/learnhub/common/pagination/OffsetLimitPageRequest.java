package com.learnhub.common.pagination;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class OffsetLimitPageRequest implements Pageable, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final int limit;
	private final long offset;
	private final Sort sort;

	private OffsetLimitPageRequest(int limit, long offset, Sort sort) {
		if (limit < 1) {
			throw new IllegalArgumentException("Limit must be greater than zero");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must not be negative");
		}

		this.limit = limit;
		this.offset = offset;
		this.sort = sort == null ? Sort.unsorted() : sort;
	}

	public static OffsetLimitPageRequest of(int limit, long offset, Sort sort) {
		return new OffsetLimitPageRequest(limit, offset, sort);
	}

	@Override
	public int getPageNumber() {
		return (int) (offset / limit);
	}

	@Override
	public int getPageSize() {
		return limit;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public Pageable next() {
		return new OffsetLimitPageRequest(limit, offset + limit, sort);
	}

	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? new OffsetLimitPageRequest(limit, offset - limit, sort) : first();
	}

	@Override
	public Pageable first() {
		return new OffsetLimitPageRequest(limit, 0, sort);
	}

	@Override
	public Pageable withPage(int pageNumber) {
		if (pageNumber < 0) {
			throw new IllegalArgumentException("Page number must not be negative");
		}
		return new OffsetLimitPageRequest(limit, (long) pageNumber * limit, sort);
	}

	@Override
	public boolean hasPrevious() {
		return offset >= limit;
	}
}
