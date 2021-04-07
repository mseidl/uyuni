import * as React from "react";
import { useEffect } from "react";
import { Text, DateTime, Radio, Select, Form } from "components/input";
import AppStreamsForm from "./appstreams/appstreams";
import { FilterFormType } from "../shared/type/filter.type";
import { clmFilterOptions, findClmFilterByKey, getClmFiltersOptions } from "../shared/business/filters.enum";
import useUserLocalization from "core/user-localization/use-user-localization";
import { Utils } from "utils/functions";
import produce from "utils/produce";

import LivePatchingFilterForm from './live-patching';

export type Props = {
  filter: Partial<FilterFormType>;
  errors: any;
  onChange: (...args: any[]) => any;
  onClientValidate: (...args: any[]) => any;
  editing?: boolean;
};

const FilterForm = (props: Props) => {
  const { timezone, localTime } = useUserLocalization();

  // If the filter type changes, resets the matcher filter
  const { editing, filter, onChange } = props;
  useEffect(() => {
    if (!editing) {
      onChange(
        produce(filter, draft => {
          const selectedFilter = findClmFilterByKey(filter.type);
          if (selectedFilter && selectedFilter.matchers.length === 1) {
            draft.matcher = selectedFilter.matchers[0].key;
          } else {
            delete draft.matcher;
          }
          if (clmFilterOptions.ADVISORY_TYPE.key === filter.type) {
            draft[clmFilterOptions.ADVISORY_TYPE.key] = "Security Advisory";
          }
          if (clmFilterOptions.ISSUE_DATE.key === filter.type) {
            draft[clmFilterOptions.ISSUE_DATE.key] = Utils.dateWithTimezone(localTime || "");
          }
        })
      );
    }
  }, [editing, filter, onChange, localTime]);

  const filterType = props.filter.type || "";
  const selectedFilter = findClmFilterByKey(filterType);
  const selectedFilterMatchers = selectedFilter?.matchers;

  return (
    <Form
      model={{ ...props.filter }}
      errors={props.errors}
      onValidate={props.onClientValidate}
      onChange={model => {
        props.onChange(model);
      }}
    >
      <React.Fragment>
        {props.editing && (
          <div className="alert alert-info" style={{ marginTop: "0px" }}>
            {t("Bear in mind that all the associated projects need to be rebuilt after a filter update")}
          </div>
        )}
        <div className="row">
          <Text
            name="filter_name"
            label={t("Filter Name")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
            disabled={props.editing}
          />
        </div>

        <Select
          name="type"
          label={t("Filter Type")}
          labelClass="col-md-3"
          divClass="col-md-6"
          required
          disabled={props.editing}
          options={getClmFiltersOptions()}
          getOptionValue={filter => filter.key}
          formatOptionLabel={filter => `${filter.entityType.text} (${filter.text})`}
        />

        {selectedFilterMatchers?.length ? (
          <Select
            name="matcher"
            label={t("Matcher")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
            disabled={props.editing}
            options={selectedFilterMatchers}
            getOptionValue={matcher => matcher.key}
            getOptionLabel={matcher => matcher.text}
          />
        ) : null}

        {clmFilterOptions.NAME.key === filterType && (
          <Text
            name={clmFilterOptions.NAME.key}
            label={t("Package Name")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
          />
        )}

        {clmFilterOptions.NEVRA.key === filterType && (
          <>
            <Text name="packageName" label={t("Package Name")} labelClass="col-md-3" divClass="col-md-6" required />
            <Text name="epoch" label={t("Epoch")} labelClass="col-md-3" divClass="col-md-6" />
            <Text name="version" label={t("Version")} labelClass="col-md-3" divClass="col-md-6" required />
            <Text name="release" label={t("Release")} labelClass="col-md-3" divClass="col-md-6" required />
            <Text name="architecture" label={t("Architecture")} labelClass="col-md-3" divClass="col-md-6" />
          </>
        )}

        {clmFilterOptions.PACKAGE_NEVR.key === filterType && (
          <>
            <Text name="packageName" label={t("Package Name")} labelClass="col-md-3" divClass="col-md-6" required />
            <Text name="epoch" label={t("Epoch")} labelClass="col-md-3" divClass="col-md-6" />
            <Text name="version" label={t("Version")} labelClass="col-md-3" divClass="col-md-6" required />
            <Text name="release" label={t("Release")} labelClass="col-md-3" divClass="col-md-6" required />
          </>
        )}

        {clmFilterOptions.ADVISORY_NAME.key === filterType && (
          <Text
            name={clmFilterOptions.ADVISORY_NAME.key}
            label={t("Advisory name")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
          />
        )}

        {clmFilterOptions.ADVISORY_TYPE.key === filterType && (
          <Radio
            name={clmFilterOptions.ADVISORY_TYPE.key}
            required
            items={[
              { label: t("Security Advisory"), value: "Security Advisory" },
              { label: t("Bug Fix Advisory"), value: "Bug Fix Advisory" },
              { label: t("Product Enhancement Advisory"), value: "Product Enhancement Advisory" },
            ]}
            label={t("Advisory Type")}
            labelClass="col-md-3"
            divClass="col-md-6"
          />
        )}

        {clmFilterOptions.ISSUE_DATE.key === filterType && (
          <DateTime
            name={clmFilterOptions.ISSUE_DATE.key}
            label={t("Issued")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
            timezone={timezone}
          />
        )}

        {clmFilterOptions.SYNOPSIS.key === filterType && (
          <Text
            name={clmFilterOptions.SYNOPSIS.key}
            label={t("Synopsis")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
          />
        )}

        {clmFilterOptions.KEYWORD.key === filterType && (
          <Radio
            name={clmFilterOptions.KEYWORD.key}
            required
            items={[
              { label: t("Reboot Required"), value: "reboot_suggested" },
              { label: t("Package Manager Restart Required"), value: "restart_suggested" },
            ]}
            openOption
            label={t("Advisory Keywords")}
            labelClass="col-md-3"
            divClass="col-md-6"
          />
        )}

        {clmFilterOptions.PACKAGE_NAME.key === filterType && (
          <Text
            name={clmFilterOptions.PACKAGE_NAME.key}
            label={t("Package Name")}
            labelClass="col-md-3"
            divClass="col-md-6"
            required
          />
        )}

        {clmFilterOptions.STREAM.key === filterType && (
          <>
            <AppStreamsForm />
          </>
        )}

        {![
          clmFilterOptions.STREAM.key,
          clmFilterOptions.LIVE_PATCHING_SYSTEM.key,
          clmFilterOptions.LIVE_PATCHING_PRODUCT.key,
        ].includes(filterType) && (
          <Radio
            inline
            name="rule"
            items={[
              { label: t("Deny"), value: "deny" },
              { label: t("Allow"), value: "allow" },
            ]}
            label={t("Rule")}
            labelClass="col-md-3"
            divClass="col-md-6"
          />
        )}

        <LivePatchingFilterForm {...props} />
      </React.Fragment>
    </Form>
  );
};

export default FilterForm;
