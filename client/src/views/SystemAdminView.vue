<script setup lang="ts">
import { computed, ref } from 'vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import Panel from '../components/Panel.vue'
import type { Permission } from '../data/customer'

type ProfilePermissions = {
  profileId: string
  name: string
  companies: Array<{
    companyId: string
    companyName: string
    permissions: Permission[]
  }>
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
  savePermissions: [
    { profileId: string; companyId: string; permissions: Permission[] },
  ]
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

const permissionAbbr: Record<Permission, string> = {
  READ_INFORMATION: 'Läs',
  APPROVE_CASES: 'God',
  ADD_EMPLOYEES: 'Add',
  CHANGE_SALARY: 'Lön',
  REGISTER_LEAVE_OF_ABSENCE: 'Tjl',
  TERMINATE_EMPLOYMENT: 'Avs',
}

const presetList = [
  { key: 'full', label: 'Full behörighet', permissions: allPermissions },
  {
    key: 'readonly',
    label: 'Läsbehörighet',
    permissions: ['READ_INFORMATION'] as Permission[],
  },
  {
    key: 'manager',
    label: 'Chef',
    permissions: [
      'READ_INFORMATION',
      'APPROVE_CASES',
      'ADD_EMPLOYEES',
    ] as Permission[],
  },
  {
    key: 'hr',
    label: 'HR',
    permissions: [
      'READ_INFORMATION',
      'ADD_EMPLOYEES',
      'REGISTER_LEAVE_OF_ABSENCE',
      'TERMINATE_EMPLOYMENT',
    ] as Permission[],
  },
  {
    key: 'finance',
    label: 'Lön',
    permissions: ['READ_INFORMATION', 'CHANGE_SALARY'] as Permission[],
  },
  { key: 'clear', label: 'Rensa', permissions: [] as Permission[] },
]

// Search & filter state
const search = ref('')
const permissionFilter = ref<Permission | ''>('')

// Selection state
const selectedIds = ref<string[]>([])

// Side panel state
const editingProfileId = ref<string | null>(null)
const selectedCompanyId = ref<string | null>(null)

const openEditor = (profileId: string): void => {
  if (editingProfileId.value !== profileId) {
    editingProfileId.value = profileId
    const profile = props.companyProfiles.find((p) => p.profileId === profileId)
    selectedCompanyId.value = profile?.companies[0]?.companyId ?? null
  }
}

const filteredProfiles = computed(() => {
  let result = props.companyProfiles
  const q = search.value.trim().toLowerCase()
  if (q) {
    result = result.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.companies.some((c) => c.companyName.toLowerCase().includes(q)),
    )
  }
  if (permissionFilter.value) {
    const pf = permissionFilter.value
    result = result.filter((p) => p.companies[0]?.permissions.includes(pf))
  }
  return result
})

const editingProfile = computed(
  () =>
    props.companyProfiles.find((p) => p.profileId === editingProfileId.value) ??
    null,
)

const activeCompany = computed(
  () =>
    editingProfile.value?.companies.find(
      (c) => c.companyId === selectedCompanyId.value,
    ) ??
    editingProfile.value?.companies[0] ??
    null,
)

const primaryPermissions = (profile: ProfilePermissions): Permission[] =>
  profile.companies[0]?.permissions ?? []

const allFilteredSelected = computed(
  () =>
    filteredProfiles.value.length > 0 &&
    filteredProfiles.value.every((p) =>
      selectedIds.value.includes(p.profileId),
    ),
)

const someFilteredSelected = computed(() =>
  filteredProfiles.value.some((p) => selectedIds.value.includes(p.profileId)),
)

const toggleSelect = (profileId: string): void => {
  if (selectedIds.value.includes(profileId)) {
    selectedIds.value = selectedIds.value.filter((id) => id !== profileId)
  } else {
    selectedIds.value = [...selectedIds.value, profileId]
  }
}

const toggleAll = (): void => {
  if (allFilteredSelected.value) {
    const visibleIds = new Set(filteredProfiles.value.map((p) => p.profileId))
    selectedIds.value = selectedIds.value.filter((id) => !visibleIds.has(id))
  } else {
    const newIds = filteredProfiles.value.map((p) => p.profileId)
    selectedIds.value = [...new Set([...selectedIds.value, ...newIds])]
  }
}

const togglePermission = (
  profile: ProfilePermissions,
  companyId: string,
  permission: Permission,
): void => {
  const company = profile.companies.find((c) => c.companyId === companyId)
  if (!company) return
  const updated = company.permissions.includes(permission)
    ? company.permissions.filter((p) => p !== permission)
    : [...company.permissions, permission]
  emit('savePermissions', {
    profileId: profile.profileId,
    companyId,
    permissions: updated,
  })
}

const applyBulkPreset = (permissions: Permission[]): void => {
  for (const profileId of selectedIds.value) {
    const profile = props.companyProfiles.find((p) => p.profileId === profileId)
    if (!profile) continue
    for (const company of profile.companies) {
      emit('savePermissions', {
        profileId,
        companyId: company.companyId,
        permissions: [...permissions],
      })
    }
  }
}

const applyPresetToCompany = (
  permissions: Permission[],
  companyId: string,
): void => {
  if (!editingProfile.value) return
  emit('savePermissions', {
    profileId: editingProfile.value.profileId,
    companyId,
    permissions: [...permissions],
  })
}
</script>

<template>
  <section>
    <PageHeader :title="title" :description="description" />

    <Panel :title="panelTitle" class="permissions-panel">
      <p class="panel-lede">{{ panelDescription }}</p>

      <EmptyState
        v-if="companyProfiles.length === 0"
        :title="t('Inga företagsanvändare')"
        :description="
          t('Det finns inga företagsanvändare i den här kundvarianten.')
        "
      />

      <template v-else>
        <!-- Toolbar -->
        <div class="toolbar">
          <input
            v-model="search"
            class="search-input"
            type="search"
            placeholder="Sök namn eller företag…"
          />
          <select v-model="permissionFilter" class="filter-select">
            <option value="">Alla behörigheter</option>
            <option v-for="perm in allPermissions" :key="perm" :value="perm">
              Har: {{ permissionLabel[perm] }}
            </option>
          </select>
          <span class="result-count">
            {{ filteredProfiles.length }} av {{ companyProfiles.length }}
          </span>
        </div>

        <!-- Bulk bar -->
        <div v-if="selectedIds.length > 0" class="bulk-bar">
          <span class="bulk-count">{{ selectedIds.length }} valda</span>
          <div class="bulk-presets">
            <button
              v-for="preset in presetList"
              :key="preset.key"
              class="preset-btn"
              @click="applyBulkPreset(preset.permissions)"
            >
              {{ preset.label }}
            </button>
          </div>
          <button class="deselect-btn" @click="selectedIds = []">
            Avmarkera
          </button>
        </div>

        <!-- Workspace -->
        <div
          class="workspace"
          :class="{ 'has-panel': editingProfileId !== null }"
        >
          <!-- Table -->
          <div class="table-section">
            <table class="user-table">
              <thead>
                <tr>
                  <th class="col-check">
                    <input
                      type="checkbox"
                      :checked="allFilteredSelected"
                      :indeterminate="
                        someFilteredSelected && !allFilteredSelected
                      "
                      @change="toggleAll"
                    />
                  </th>
                  <th class="col-name-h">Namn</th>
                  <th class="col-companies-h">Företag</th>
                  <th class="col-badges-h">Behörigheter</th>
                  <th class="col-action-h"></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="profile in filteredProfiles"
                  :key="profile.profileId"
                  class="user-row"
                  :class="{
                    'is-selected': selectedIds.includes(profile.profileId),
                    'is-editing': editingProfileId === profile.profileId,
                  }"
                  @click="openEditor(profile.profileId)"
                >
                  <td class="col-check" @click.stop>
                    <input
                      type="checkbox"
                      :checked="selectedIds.includes(profile.profileId)"
                      @change="toggleSelect(profile.profileId)"
                    />
                  </td>
                  <td class="col-name">{{ profile.name }}</td>
                  <td class="col-companies">
                    {{ profile.companies.map((c) => c.companyName).join(', ') }}
                  </td>
                  <td class="col-badges">
                    <span
                      v-for="perm in allPermissions"
                      :key="perm"
                      class="perm-dot"
                      :class="{
                        active: primaryPermissions(profile).includes(perm),
                      }"
                      :title="permissionLabel[perm]"
                    >
                      {{ permissionAbbr[perm] }}
                    </span>
                  </td>
                  <td class="col-action">
                    <button
                      class="edit-btn"
                      @click.stop="openEditor(profile.profileId)"
                    >
                      Redigera
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <p v-if="filteredProfiles.length === 0" class="no-results">
              Inga användare matchar sökningen.
            </p>
          </div>

          <!-- Side panel -->
          <aside v-if="editingProfile" class="editor-panel">
            <div class="editor-header">
              <strong class="editor-name">{{ editingProfile.name }}</strong>
              <button
                class="close-btn"
                aria-label="Stäng"
                @click="editingProfileId = null"
              >
                ✕
              </button>
            </div>

            <!-- Company tab switcher (only shown when >1 company) -->
            <div
              v-if="editingProfile.companies.length > 1"
              class="company-tabs"
            >
              <button
                v-for="company in editingProfile.companies"
                :key="company.companyId"
                class="company-tab"
                :class="{
                  'is-active': company.companyId === activeCompany?.companyId,
                }"
                @click="selectedCompanyId = company.companyId"
              >
                {{ company.companyName }}
              </button>
            </div>

            <!-- Active company permissions -->
            <div v-if="activeCompany" class="editor-company-body">
              <div class="editor-presets">
                <span class="presets-label">Snabbval:</span>
                <button
                  v-for="preset in presetList"
                  :key="preset.key"
                  class="preset-chip"
                  @click="
                    applyPresetToCompany(
                      preset.permissions,
                      activeCompany.companyId,
                    )
                  "
                >
                  {{ preset.label }}
                </button>
              </div>
              <div class="editor-permission-list">
                <label
                  v-for="perm in allPermissions"
                  :key="perm"
                  class="editor-permission-row"
                >
                  <input
                    type="checkbox"
                    :checked="activeCompany.permissions.includes(perm)"
                    @change="
                      togglePermission(
                        editingProfile,
                        activeCompany.companyId,
                        perm,
                      )
                    "
                  />
                  {{ permissionLabel[perm] }}
                </label>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </Panel>
  </section>
</template>

<style scoped>
.panel-lede {
  max-width: 64ch;
  margin: 0 0 20px;
  color: var(--muted);
  line-height: 1.55;
}

.permissions-panel {
  margin-top: 24px;
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  font-size: 0.875rem;
  background: var(--surface);
  color: var(--ink);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent, #0057b7);
  box-shadow: 0 0 0 2px
    color-mix(in srgb, var(--accent, #0057b7) 20%, transparent);
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  font-size: 0.875rem;
  background: var(--surface);
  color: var(--ink);
  cursor: pointer;
}

.result-count {
  font-size: 0.8rem;
  color: var(--muted);
  white-space: nowrap;
}

/* Bulk bar */
.bulk-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: color-mix(in srgb, var(--accent, #0057b7) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--accent, #0057b7) 25%, transparent);
  border-radius: var(--radius-control, 6px);
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.bulk-count {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
}

.bulk-presets {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.preset-btn {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink);
  transition: background 0.15s;
}

.preset-btn:hover {
  background: var(--surface-hover, var(--border));
}

.deselect-btn {
  padding: 4px 10px;
  border: none;
  background: none;
  font-size: 0.8rem;
  color: var(--muted);
  cursor: pointer;
  text-decoration: underline;
  white-space: nowrap;
}

/* Workspace */
.workspace {
  display: flex;
  gap: 0;
  align-items: flex-start;
}

.workspace.has-panel {
  gap: 16px;
}

.table-section {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

/* Table */
.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.user-table thead th {
  text-align: left;
  padding: 8px 10px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.user-table tbody tr {
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.1s;
}

.user-table tbody tr:last-child {
  border-bottom: none;
}

.user-table tbody tr:hover {
  background: var(--surface-hover, var(--border));
}

.user-table tbody tr.is-editing {
  background: color-mix(in srgb, var(--accent, #0057b7) 6%, transparent);
}

.user-table tbody tr.is-selected {
  background: color-mix(in srgb, var(--accent, #0057b7) 4%, transparent);
}

.user-table td {
  padding: 9px 10px;
  vertical-align: middle;
}

.col-check {
  width: 36px;
  text-align: center;
}

.col-name {
  font-weight: 600;
  white-space: nowrap;
}

.col-companies {
  color: var(--muted);
  font-size: 0.82rem;
}

.col-badges {
  white-space: nowrap;
}

/* Permission badges */
.perm-dot {
  display: inline-block;
  padding: 2px 5px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  margin-right: 3px;
  background: var(--border);
  color: var(--muted);
  transition:
    background 0.1s,
    color 0.1s;
}

.perm-dot.active {
  background: color-mix(in srgb, var(--accent, #0057b7) 15%, transparent);
  color: var(--accent, #0057b7);
}

.col-action {
  text-align: right;
  white-space: nowrap;
}

.edit-btn {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  background: var(--surface);
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink);
}

.edit-btn:hover {
  background: var(--surface-hover, var(--border));
}

.no-results {
  padding: 24px 0;
  text-align: center;
  color: var(--muted);
  font-size: 0.875rem;
}

/* Side panel */
.editor-panel {
  width: 320px;
  flex-shrink: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  background: var(--surface-subtle, var(--canvas, #f8f9fa));
  overflow: hidden;
  position: sticky;
  top: 16px;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}

.editor-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
}

.editor-companies-label {
  font-size: 0.8rem;
  color: var(--muted);
}

.close-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--muted);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-btn:hover {
  background: var(--border);
  color: var(--ink);
}

.editor-presets {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.presets-label {
  font-size: 0.75rem;
  color: var(--muted);
  white-space: nowrap;
}

.preset-chip {
  padding: 3px 8px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  font-size: 0.75rem;
  cursor: pointer;
  color: var(--ink);
  transition: background 0.15s;
}

.preset-chip:hover {
  background: color-mix(in srgb, var(--accent, #0057b7) 10%, transparent);
  border-color: var(--accent, #0057b7);
  color: var(--accent, #0057b7);
}

/* Company tab switcher */
.company-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  overflow-x: auto;
  scrollbar-width: none;
}

.company-tabs::-webkit-scrollbar {
  display: none;
}

.company-tab {
  flex-shrink: 0;
  padding: 10px 14px;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition:
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.company-tab:hover {
  color: var(--ink);
}

.company-tab.is-active {
  color: var(--accent, #0057b7);
  border-bottom-color: var(--accent, #0057b7);
  font-weight: 600;
}

.editor-company-body {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.editor-permission-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.editor-permission-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 2px 0;
}

.editor-permission-row input[type='checkbox'] {
  cursor: pointer;
  accent-color: var(--accent, #0057b7);
}
</style>
