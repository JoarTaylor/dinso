<script setup lang="ts">
import { computed, ref } from 'vue'
import DataTable, { type DataTableColumn } from '../components/DataTable.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import Panel from '../components/Panel.vue'
import type { Permission } from '../data/customer'

type ProfilePermissions = {
  profileId: string
  name: string
  companies: Array<{ companyId: string; companyName: string; permissions: Permission[] }>
}

const props = defineProps<{
  title: string
  description: string
  panelTitle: string
  panelDescription: string
  rows: Record<string, string>[]
  companyProfiles: ProfilePermissions[]
  t: (source: string) => string
}>()

const emit = defineEmits<{
  savePermissions: [{ profileId: string; companyId: string; permissions: Permission[] }]
}>()

const allPermissions: Permission[] = [
  'READ_INFORMATION',
  'APPROVE_CASES',
  'ADD_EMPLOYEES',
  'CHANGE_SALARY',
  'REGISTER_LEAVE_OF_ABSENCE',
  'TERMINATE_EMPLOYMENT',
]

const permissionLabel: Record<Permission, string> = {
  READ_INFORMATION: 'Läs information',
  APPROVE_CASES: 'Godkänn ärenden',
  ADD_EMPLOYEES: 'Lägg till medarbetare',
  CHANGE_SALARY: 'Ändra lön',
  REGISTER_LEAVE_OF_ABSENCE: 'Registrera tjänstledighet',
  TERMINATE_EMPLOYMENT: 'Avsluta anställning',
}

const columns = computed<DataTableColumn[]>(() => [
  { key: 'name', label: props.t('Företagsanvändare') },
  { key: 'companies', label: props.t('Företag') },
])

const expandedProfile = ref<string | null>(null)

const toggleProfile = (profileId: string): void => {
  expandedProfile.value = expandedProfile.value === profileId ? null : profileId
}

const hasPermission = (profile: ProfilePermissions, companyId: string, permission: Permission): boolean => {
  const company = profile.companies.find((c) => c.companyId === companyId)
  return company?.permissions.includes(permission) ?? false
}

const togglePermission = (profile: ProfilePermissions, companyId: string, permission: Permission): void => {
  const company = profile.companies.find((c) => c.companyId === companyId)
  if (!company) return
  const current = company.permissions.includes(permission)
  const updated = current
    ? company.permissions.filter((p) => p !== permission)
    : [...company.permissions, permission]
  emit('savePermissions', { profileId: profile.profileId, companyId, permissions: updated })
}
</script>

<template>
  <section>
    <PageHeader :title="title" :description="description" />

    <Panel :title="panelTitle">
      <p class="panel-lede">{{ panelDescription }}</p>
      <EmptyState
        v-if="rows.length === 0"
        :title="t('Inga företagsanvändare')"
        :description="t('Det finns inga företagsanvändare i den här kundvarianten.')"
      />
      <DataTable
        v-else
        :caption="panelTitle"
        :columns="columns"
        :rows="rows"
      >
        <template #cell-name="{ row }">
          <strong>{{ row.name }}</strong>
        </template>
      </DataTable>
    </Panel>

    <Panel v-if="companyProfiles.length > 0" :title="t('Behörigheter')" class="permissions-panel">
      <p class="panel-lede">{{ t('Hantera vilka åtgärder varje användare kan utföra per företag.') }}</p>
      <div class="profile-list">
        <div
          v-for="profile in companyProfiles"
          :key="profile.profileId"
          class="profile-item"
        >
          <button
            class="profile-header"
            :aria-expanded="expandedProfile === profile.profileId"
            @click="toggleProfile(profile.profileId)"
          >
            <span class="profile-name">{{ profile.name }}</span>
            <span class="profile-companies">
              {{ profile.companies.map((c) => c.companyName).join(', ') }}
            </span>
            <span class="expand-icon" aria-hidden="true">
              {{ expandedProfile === profile.profileId ? '▲' : '▼' }}
            </span>
          </button>
          <div v-if="expandedProfile === profile.profileId" class="permission-editor">
            <div
              v-for="company in profile.companies"
              :key="company.companyId"
              class="company-permissions"
            >
              <h4 class="company-name">{{ company.companyName }}</h4>
              <div class="permission-grid">
                <label
                  v-for="perm in allPermissions"
                  :key="perm"
                  class="permission-row"
                >
                  <input
                    type="checkbox"
                    :checked="hasPermission(profile, company.companyId, perm)"
                    @change="togglePermission(profile, company.companyId, perm)"
                  />
                  {{ permissionLabel[perm] }}
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Panel>
  </section>
</template>

<style scoped>
.panel-lede {
  max-width: 64ch;
  margin: 0 0 24px;
  color: var(--muted);
  line-height: 1.55;
}

.permissions-panel {
  margin-top: 24px;
}

.profile-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  overflow: hidden;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 14px 16px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  font-size: 0.95rem;
  color: var(--ink);
}

.profile-header:hover {
  background: var(--surface-hover, var(--border));
}

.profile-name {
  font-weight: 600;
  min-width: 140px;
}

.profile-companies {
  color: var(--muted);
  flex: 1;
}

.expand-icon {
  font-size: 0.75rem;
  color: var(--muted);
}

.permission-editor {
  border-top: 1px solid var(--border);
  padding: 16px;
  background: var(--surface-subtle, var(--canvas, #f8f9fa));
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.company-permissions + .company-permissions {
  border-top: 1px solid var(--border);
  padding-top: 20px;
}

.company-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--muted);
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
}

.permission-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 4px 0;
}

.permission-row input[type='checkbox'] {
  cursor: pointer;
  accent-color: var(--accent, #0057b7);
}
</style>
