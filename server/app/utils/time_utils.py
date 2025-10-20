"""Time-related utility functions."""

# Time increment for break duration rounding
TIME_INCREMENT_MINUTES = 5


def round_up_to_nearest_5_minutes(duration: int) -> int:
    """Round up break duration to the nearest 5-minute increment.

    Args:
        duration: Duration in minutes to round up

    Returns:
        Duration rounded up to nearest 5-minute increment
    """
    if duration <= 0:
        return 0

    return ((duration - 1) // TIME_INCREMENT_MINUTES + 1) * TIME_INCREMENT_MINUTES
