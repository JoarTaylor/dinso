<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import customer from '@customer/config'
import { activeLocale, translate } from '../i18n'
import { statusTone } from '../data/status'
import { useDemoSessionStore } from '../stores/demoSession'
import { useDemoPortalStore } from '../stores/demoPortal'
import type { Permission } from '../data/customer'
import OverviewView from './OverviewView.vue'
import InsuranceView from './InsuranceView.vue'
import EmployeesView from './EmployeesView.vue'
import PlansView from './PlansView.vue'
import CasesView from './CasesView.vue'
import ActivityView from './ActivityView.vue'
import SystemAdminView from './SystemAdminView.vue'
import AddEmployeeFlow from '../components/AddEmployeeFlow.vue'
import CaseModal from '../components/CaseModal.vue'
import DemoNotice from '../components/DemoNotice.vue'

const route = useRoute()
const router = useRouter()
const session = useDemoSessionStore()
const portal = useDemoPortalStore()
const t = (source: string, values?: Record<string, string | number>): string =>
  translate(activeLocale.value, source, values)
const page = computed(() => route.meta.page ?? 'overview')
const selectedCase = ref<{
  id: string
  name: string
  status: string
  value: string
  detail: string
} | null>(null)
const isCompany = computed(() => session.isCompany)
const overviewTitle = computed(() =>
  isCompany.value
    ? session.selectedCompanyName
    : t('Hej {name}', { name: session.profile?.name.split(' ')[0] ?? '' }),
)
const activityTitle = computed(() =>
  page.value === 'documents'
    ? t('Dokument')
    : page.value === 'payments'
      ? t('Utbetalningar')
      : t('Händelser'),
)
const activityDescription = computed(() =>
  page.value === 'documents'
    ? t('Tillgängliga dokument med lokal metadata.')
    : page.value === 'payments'
      ? t('Kommande, pågående och avslutade utbetalningar.')
      : t('Senaste händelser och uppdateringar.'),
)
const activityRows = computed(() =>
  page.value === 'documents' ? portal.documentRows : portal.activityRows,
)
const systemAdminDescription = computed(() =>
  t('Se företagsadministratörer och vilka företag de hanterar.'),
)
const systemAdminPanelDescription = computed(() =>
  t('Här visas företagsadministratörer för den valda organisationen.'),
)
const overviewDescription = computed(() =>
  t(
    isCompany.value
      ? 'Följ planer, medarbetare och sådant som behöver hanteras.'
      : 'Din pension och dina försäkringar – samlade på ett ställe.',
  ),
)
const overviewBottomMetrics = computed(
  () =>
    !isCompany.value && customer.privateOverviewMetricLayout === 'bottom-bar',
)
const overviewRows = computed(() =>
  isCompany.value ? portal.cases.slice(0, 5) : portal.insurance,
)
const overviewNextStep = computed(() => ({
  description: t(
    isCompany.value
      ? 'Granska den registrerade löneändringen innan den 18 september.'
      : 'Se hur ditt innehav är fördelat och justera fondvikter.',
  ),
  action: t(isCompany.value ? 'Visa ärenden' : 'Visa försäkringar'),
}))
const insuranceDescription = computed(() =>
  t('Detaljer om sparande, skydd och val för varje försäkring.'),
)
const employeeDescription = computed(() =>
  t('Sök och följ anställningar inom {company}.', {
    company: session.selectedCompanyName,
  }),
)
const caseModalLabels = computed(() => ({
  eyebrow: t('Ärende'),
  close: t('Stäng'),
  status: t('Status'),
  due: t('Förfaller'),
  details: t('Detaljer'),
  approve: t('Godkänn ärende'),
  approved: t('Godkänd'),
  readOnly: t('Du har läsbehörighet och kan inte godkänna ärenden.'),
}))
const activityPage = computed(
  () => page.value as 'events' | 'documents' | 'payments',
)
const useApi = import.meta.env.VITE_USE_API === 'true'
const apiUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '') ?? ''

type ProfilePermissions = {
  profileId: string
  name: string
  companies: Array<{
    companyId: string
    companyName: string
    permissions: Permission[]
  }>
}

const companyProfiles = ref<ProfilePermissions[]>(
  customer.profiles
    .filter((item) => item.portal === 'COMPANY')
    .map((item) => ({
      profileId: item.id,
      name: item.name,
      companies: (item.companies?.length
        ? item.companies
        : item.company
          ? [item.company]
          : []
      ).map((name) => ({
        companyId: name,
        companyName: name,
        permissions: (item.permissions ?? []) as Permission[],
      })),
    })),
)

const companyAdmins = computed(() =>
  companyProfiles.value.map((item) => ({
    name: item.name,
    companies:
      item.companies.map((c) => c.companyName).join(', ') ||
      t('Inga företag kopplade'),
  })),
)

const savePermissions = async (
  profileId: string,
  companyId: string,
  permissions: Permission[],
): Promise<void> => {
  const profile = companyProfiles.value.find((p) => p.profileId === profileId)
  if (!profile) return
  const company = profile.companies.find((c) => c.companyId === companyId)
  if (company) company.permissions = permissions
  session.updateProfileCompanies(
    profileId,
    profile.companies.map((c) => ({ id: c.companyId, name: c.companyName, permissions: c.permissions })),
  )
  if (useApi && session.sessionToken) {
    await fetch(
      `${apiUrl}/api/system/profiles/${encodeURIComponent(profileId)}/companies/${encodeURIComponent(companyId)}/permissions`,
      {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${session.sessionToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ permissions }),
      },
    ).catch(() => undefined)
  }
}

const loadSystemProfiles = async (): Promise<void> => {
  if (!useApi || !session.sessionToken) return
  try {
    const response = await fetch(`${apiUrl}/api/system/profiles`, {
      headers: { Authorization: `Bearer ${session.sessionToken}` },
    })
    if (!response.ok) return
    const data = (await response.json()) as Array<{
      profileId: string
      name: string
      companies: Array<{
        companyId: string
        companyName: string
        permissions: string[]
      }>
    }>
    companyProfiles.value = data.map((p) => ({
      profileId: p.profileId,
      name: p.name,
      companies: p.companies.map((c) => ({
        companyId: c.companyId,
        companyName: c.companyName,
        permissions: c.permissions as Permission[],
      })),
    }))
  } catch (e) {
    console.log(e)
  }
}
const firstEmployeeAction = computed<'salary' | 'leave' | 'end'>(() =>
  session.canChangeSalary ? 'salary' : session.canRegisterLeave ? 'leave' : 'end',
)

const navigate = (target: 'insurance' | 'cases'): void => {
  void router.push({
    name: target === 'insurance' ? 'private-insurance' : 'company-cases',
  })
}
const selectCase = (name: string): void => {
  selectedCase.value = portal.cases.find((item) => item.name === name) ?? null
}
const approveSelectedCase = async (): Promise<void> => {
  if (selectedCase.value) await portal.approveCase(selectedCase.value)
  selectedCase.value = null
}
onMounted(() => {
  if (session.isCompany) void portal.loadCompanyData()
  else if (session.activePortal === 'SYSTEM') void loadSystemProfiles()
  else void portal.loadFundAllocation()
})

const finishEmployee = async (draft: {
  name: string
  planId: string
  salary: number
  startsOn: string
}): Promise<string | undefined> => {
  const result = await portal.addEmployee(draft)
  if (!result) await router.push({ name: 'company-employees' })
  return result
}
</script>

<template>
  <SystemAdminView
    v-if="session.activePortal === 'SYSTEM'"
    :title="t('Systemadmin')"
    :description="systemAdminDescription"
    :panel-title="t('Företagsanvändare')"
    :panel-description="systemAdminPanelDescription"
    :rows="companyAdmins"
    :company-profiles="companyProfiles"
    :t="t"
    @save-permissions="
      savePermissions($event.profileId, $event.companyId, $event.permissions)
    "
  />
  <OverviewView
    v-else-if="page === 'overview'"
    :is-company="isCompany"
    :title="overviewTitle"
    :description="overviewDescription"
    :metrics="portal.overviewMetrics"
    :bottom-metrics="overviewBottomMetrics"
    :rows="overviewRows"
    :next-step="overviewNextStep"
    :t="t"
    :status-tone="statusTone"
    @navigate="navigate"
    @open-case="selectCase($event.name)"
  />
  <InsuranceView
    v-else-if="page === 'insurance'"
    :title="t('Försäkringar')"
    :description="insuranceDescription"
    :insurance="portal.insurance"
    :allocation="portal.allocation"
    :confirmed="portal.confirmed"
    :error="portal.error"
    :max-funds="customer.rules.maxFunds"
    :t="t"
    :status-tone="statusTone"
    @update:allocation="portal.allocation = $event"
    @confirm="portal.confirmAllocation"
  />
  <EmployeesView
    v-else-if="page === 'employees'"
    :title="t('Medarbetare')"
    :description="employeeDescription"
    :employees="portal.filteredEmployees"
    :can-add-employees="session.canAddEmployees"
    :can-change-salary="session.canChangeSalary"
    :can-register-leave="session.canRegisterLeave"
    :can-terminate-employment="session.canTerminateEmployment"
    :search="portal.search"
    :selected-employee="portal.selectedEmployee"
    :employee-action="portal.employeeAction"
    :salary-draft="portal.salaryDraft"
    :leave-reason="portal.leaveReason"
    :leave-until="portal.leaveUntil"
    :end-date="portal.endDate"
    :leave-reasons="portal.leaveReasons"
    :saving="portal.saving"
    :error="portal.error"
    :t="t"
    :status-tone="statusTone"
    @update:search="portal.search = $event"
    @update:employee-action="portal.employeeAction = $event"
    @update:salary-draft="portal.salaryDraft = $event"
    @update:leave-reason="portal.leaveReason = $event"
    @update:leave-until="portal.leaveUntil = $event"
    @update:end-date="portal.endDate = $event"
    @manage="(emp) => { portal.manageEmployee(emp); portal.employeeAction = firstEmployeeAction }"
    @cancel="portal.selectedEmployee = null"
    @add="router.push({ name: 'company-add-employee' })"
    @save="portal.saveEmployee"
  />
  <AddEmployeeFlow
    v-else-if="page === 'add-employee'"
    :plans="portal.planOptions"
    :t="t"
    :on-submit="finishEmployee"
    @cancel="router.push({ name: 'company-employees' })"
  />
  <PlansView
    v-else-if="page === 'plans'"
    :title="t('Pensionsplaner')"
    :description="t('Avtal, premieflöden och anslutna medarbetare.')"
    :plans="portal.plans"
    :t="t"
  />
  <CasesView
    v-else-if="page === 'cases'"
    :title="t('Ärenden')"
    :cases="portal.cases"
    :t="t"
    :status-tone="statusTone"
    @open="selectCase($event.name)"
  />
  <ActivityView
    v-else
    :page="activityPage"
    :title="activityTitle"
    :description="activityDescription"
    :rows="activityRows"
    :t="t"
    :status-tone="statusTone"
  />
  <DemoNotice v-if="portal.message">{{ portal.message }}</DemoNotice>
  <CaseModal
    v-if="selectedCase"
    :item="selectedCase"
    :can-approve="session.canApproveCases"
    :labels="caseModalLabels"
    @close="selectedCase = null"
    @approve="approveSelectedCase"
  />
</template>
