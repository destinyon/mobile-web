<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight } from 'lucide-vue-next';

const props = defineProps<{
  page: number;
  pageSize: number;
  total: number;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  change: [page: number];
}>();

function go(delta: number): void {
  const next = props.page + delta;
  if (next >= 1 && next <= totalPages.value && !props.disabled) {
    emit('change', next);
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
</script>

<template>
  <div class="pagination-bar">
    <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
    <div class="pager-actions">
      <button type="button" title="上一页" :disabled="disabled || page <= 1" @click="go(-1)">
        <ChevronLeft :size="16" />
      </button>
      <button type="button" title="下一页" :disabled="disabled || page >= totalPages" @click="go(1)">
        <ChevronRight :size="16" />
      </button>
    </div>
  </div>
</template>
