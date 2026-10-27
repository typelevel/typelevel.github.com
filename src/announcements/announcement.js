function initAnnouncementCountdowns() {
  document.querySelectorAll("[data-announcement-countdown]").forEach((countdown) => {
    const announcement = countdown.closest(".homepage-announcement") || countdown;
    const location = countdown.querySelector("[data-announcement-countdown-location]");
    const daysElement = countdown.querySelector("[data-announcement-countdown-days]");
    const hoursElement = countdown.querySelector("[data-announcement-countdown-hours]");
    const minutesElement = countdown.querySelector("[data-announcement-countdown-minutes]");
    const description = countdown.querySelector("[data-announcement-countdown-description]");

    if (
      [location, daysElement, hoursElement, minutesElement, description].some(
        (element) => !element,
      )
    ) {
      return;
    }

    const events = Array.from(announcement.querySelectorAll("[data-announcement-event]"))
      .map((event) => ({
        location: event.dataset.eventLocation || "the next event",
        start: Date.parse(event.dateTime),
      }))
      .filter((event) => Number.isFinite(event.start))
      .sort((first, second) => first.start - second.start);
    let interval;

    const formatUnit = (value, unit) =>
      `${value} ${unit}${value === 1 ? "" : "s"}`;

    function updateCountdown() {
      const now = Date.now();
      const nextEvent = events.find((event) => event.start >= now);

      if (!nextEvent) {
        countdown.hidden = true;
        if (interval) window.clearInterval(interval);
        return;
      }

      const totalMinutes = Math.max(0, Math.floor((nextEvent.start - now) / 60000));
      const days = Math.floor(totalMinutes / 1440);
      const hours = Math.floor((totalMinutes % 1440) / 60);
      const minutes = totalMinutes % 60;

      location.textContent = nextEvent.location;
      daysElement.textContent = days;
      hoursElement.textContent = hours;
      minutesElement.textContent = minutes;
      description.textContent =
        `${formatUnit(days, "day")}, ${formatUnit(hours, "hour")}, and ` +
        `${formatUnit(minutes, "minute")} until the ${nextEvent.location} event.`;
      countdown.hidden = false;
    }

    updateCountdown();
    if (!countdown.hidden) interval = window.setInterval(updateCountdown, 60000);
  });
}

initAnnouncementCountdowns();
