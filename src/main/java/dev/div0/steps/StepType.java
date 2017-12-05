package dev.div0.steps;

public enum StepType {
	OPEN_URL,
	CLICK_LINK,
	SWITCH_TO_IFRAME,
	FIND_IFRAME_BY_TAG_AND_TAG_INDEX,
	GET_DEFAULT_CONTENT,
	SELECT_DROP_DOWN_ITEM,
	TAKE_IFRAME_SCREENSHOT,
	WAIT,
	START_RECAPTCHA_ROBOT,
	GET_RECAPTCHA_INSTRUCTIONS,
	SAVE_RECAPTCHA_IMAGE,
	SOLVE_RECAPTCHA,
	SOLVE_RECAPTCHA_WITH_ERROR,
	DETECT_DATES_AVAILABLE_FOR_SELECTED_CITY,
	BRING_BROWSER_TO_FRONT,
	REFRESH_PAGE,
	CLICK_ELEMENT;
}